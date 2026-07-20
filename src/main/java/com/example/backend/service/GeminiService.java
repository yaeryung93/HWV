package com.example.backend.service;

import com.example.backend.dto.JavaAnalysisResponse;
import com.example.backend.dto.CodingProblemDraft;
import com.example.backend.dto.CodingReviewResponse;
import com.example.backend.entity.Quiz;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;

@Service
public class GeminiService {
    private static final Logger log = LoggerFactory.getLogger(GeminiService.class);
    @Value("${gemini.api.key}") private String apiKey;
    private final WebClient webClient = WebClient.builder().baseUrl("https://generativelanguage.googleapis.com").build();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final JavaSyntaxDetector syntaxDetector;

    public GeminiService(JavaSyntaxDetector syntaxDetector) {
        this.syntaxDetector = syntaxDetector;
    }

    private String callGemini(String prompt) {
        Map<String, Object> body = Map.of(
            "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))),
            "generationConfig", Map.of("responseMimeType", "application/json", "temperature", 0.2)
        );
        Map<?, ?> response = webClient.post()
            .uri("/v1beta/models/gemini-flash-latest:generateContent")
            .header("X-goog-api-key", apiKey).contentType(MediaType.APPLICATION_JSON)
            .bodyValue(body).retrieve().bodyToMono(Map.class).block();
        if (response == null) throw new IllegalStateException("Gemini 응답이 없습니다.");
        List<?> candidates = (List<?>) response.get("candidates");
        if (candidates == null || candidates.isEmpty()) throw new IllegalStateException("Gemini가 결과를 생성하지 못했습니다.");
        Map<?, ?> content = (Map<?, ?>) ((Map<?, ?>) candidates.get(0)).get("content");
        List<?> parts = (List<?>) content.get("parts");
        return String.valueOf(((Map<?, ?>) parts.get(0)).get("text"));
    }

    private JsonNode readGeminiJson(String response) throws Exception {
        String cleaned = response.trim();
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.replaceFirst("^```(?:json)?\\s*", "").replaceFirst("\\s*```$", "");
        }
        int objectStart = cleaned.indexOf('{');
        int arrayStart = cleaned.indexOf('[');
        int start = objectStart < 0 ? arrayStart : arrayStart < 0 ? objectStart : Math.min(objectStart, arrayStart);
        if (start < 0) throw new IllegalStateException("Gemini 응답에서 JSON을 찾을 수 없습니다.");

        char opening = cleaned.charAt(start);
        char closing = opening == '{' ? '}' : ']';
        int end = cleaned.lastIndexOf(closing);
        if (end < start) throw new IllegalStateException("Gemini JSON 응답이 완전하지 않습니다.");
        return objectMapper.readTree(cleaned.substring(start, end + 1));
    }

    public JavaAnalysisResponse analyzeCode(String code) {
        List<JavaSyntaxDetector.Detected> detected = syntaxDetector.detect(code);
        String allowed = objectMapper.valueToTree(detected).toString();
        String prompt = """
            다음 Java 코드에서 서버가 실제로 탐지한 문법 3개만 설명하라.
            목록에 없는 Java 일반 지식이나 문법을 절대 추가하지 마라.
            각 설명은 이 코드에서 해당 문법이 어떻게 쓰였는지 구체적으로 설명한다.
            JSON 형식: {"summary":"코드 요약","grammars":[{"name":"탐지 이름 그대로","description":"설명","rating":1~5,"evidence":"탐지 근거 그대로"}]}
            탐지 목록: %s
            Java 코드:
            %s
            """.formatted(allowed, code);
        try {
            JsonNode root = readGeminiJson(callGemini(prompt));
            List<JavaAnalysisResponse.Grammar> grammars = new ArrayList<>();
            JsonNode nodes = root.path("grammars");
            for (int i = 0; i < detected.size(); i++) {
                JavaSyntaxDetector.Detected item = detected.get(i);
                JsonNode node = i < nodes.size() ? nodes.get(i) : null;
                String description = node == null ? item.name() + " 문법이 업로드한 코드에서 사용되었습니다." : node.path("description").asText();
                int rating = node == null ? 3 : Math.max(1, Math.min(5, node.path("rating").asInt(3)));
                grammars.add(new JavaAnalysisResponse.Grammar(item.name(), description, rating, item.evidence()));
            }
            return new JavaAnalysisResponse(root.path("summary").asText("업로드한 Java 코드를 분석했습니다."), grammars, code);
        } catch (Exception e) {
            throw new IllegalStateException("Gemini 분석 결과를 처리하지 못했습니다.", e);
        }
    }

    public List<Quiz> generateQuiz(String code) {
        List<JavaSyntaxDetector.Detected> detected = syntaxDetector.detect(code);
        Set<String> allowedNames = new LinkedHashSet<>();
        detected.forEach(item -> allowedNames.add(item.name()));
        String prompt = """
            업로드된 Java 코드에 실제로 존재한다고 서버가 검증한 문법만 사용해 객관식 문제 5개를 생성하라.
            Java 일반 상식 문제, 코드에 없는 문법, 탐지 목록 밖의 주제는 절대 출제하지 마라.
            모든 문제는 질문 또는 해설에서 업로드 코드의 구체적인 표현을 언급해야 한다.
            정답 answer는 option1~option5 중 1부터 5까지의 번호다.
            JSON 형식: {"quizzes":[{"grammarName":"탐지 이름 그대로","question":"질문","option1":"","option2":"","option3":"","option4":"","option5":"","answer":1,"explanation":"코드 근거가 포함된 해설"}]}
            정확히 5개를 생성하라.
            탐지 목록: %s
            Java 코드:
            %s
            """.formatted(objectMapper.valueToTree(detected), code);
        try {
            JsonNode quizzes = readGeminiJson(callGemini(prompt)).path("quizzes");
            if (quizzes.size() != 5) throw new IllegalStateException("Gemini가 문제 5개를 반환하지 않았습니다.");
            List<Quiz> result = new ArrayList<>();
            for (JsonNode node : quizzes) {
                String grammarName = node.path("grammarName").asText();
                if (!allowedNames.contains(grammarName)) throw new IllegalStateException("코드에 없는 문법이 문제에 포함되었습니다: " + grammarName);
                int answer = node.path("answer").asInt();
                if (answer < 1 || answer > 5) throw new IllegalStateException("정답 번호가 올바르지 않습니다.");
                Quiz quiz = new Quiz();
                quiz.setGrammarName(grammarName); quiz.setQuestion(node.path("question").asText());
                quiz.setOption1(node.path("option1").asText()); quiz.setOption2(node.path("option2").asText());
                quiz.setOption3(node.path("option3").asText()); quiz.setOption4(node.path("option4").asText()); quiz.setOption5(node.path("option5").asText());
                quiz.setAnswer(answer); quiz.setExplanation(node.path("explanation").asText()); result.add(quiz);
            }
            return result;
        } catch (Exception e) {
            if (e instanceof IllegalStateException state) throw state;
            throw new IllegalStateException("Gemini 문제 생성 결과를 처리하지 못했습니다.", e);
        }
    }

    public String analyzeAndGenerate(String code) {
        return callGemini(code);
    }
    public String summarize(String text) {
        return callGemini("다음 내용을 한국어 JSON으로 요약해줘: " + text);
    }

    public List<CodingProblemDraft> generateCodingProblems(String code) {
        List<JavaSyntaxDetector.Detected> detected = syntaxDetector.detect(code);
        if (detected.size() < 3) {
            throw new IllegalArgumentException("업로드한 코드에서 코딩 문제를 만들 핵심 Java 문법 3개를 찾지 못했습니다.");
        }
        List<JavaSyntaxDetector.Detected> selected = detected.subList(0, 3);
        String prompt = """
            아래 Java 코드에서 서버가 검증한 핵심 문법 3개를 활용하는 프로그래머스 형식의 Java 코딩 문제를 생성하라.
            탐지 목록 순서대로 문법 하나당 문제 하나를 만들고 problems 배열에도 같은 순서로 정확히 3개를 넣는다.
            grammarName은 서버가 순서에 따라 지정하므로 출력하지 않아도 된다.
            객관식, Java 일반 상식, 개념 설명, 원본 코드나 변수명을 맞히는 문제는 절대 만들지 않는다.
            각 문제는 사용자가 Java 메서드를 직접 구현하는 코딩 테스트여야 하며, 해당 문법을 실제로 활용해야 해결할 수 있어야 한다.
            title, description, requirements, inputExample, outputExample, starterCode를 구체적으로 작성한다.
            title은 80자 이내, description은 1,000자 이내로 핵심만 설명하고 같은 내용을 반복하지 않는다.
            requirements의 각 항목은 300자 이내로 작성하며 문제당 최대 5개만 만든다.
            starterCode에는 컴파일 가능한 class Solution과 구현할 메서드 선언을 포함하되 정답 구현은 넣지 않는다.
            각 문제에는 기본값, 경계값, 일반값을 포함한 서로 다른 테스트케이스를 정확히 3개 만든다.
            모든 테스트케이스의 name, input, expected를 비워 두지 말고 입력과 정확한 기대 출력을 명시한다.
            input과 expected는 JSON 객체나 배열이 아니라 사람이 읽을 수 있는 짧은 문자열로 작성한다.
            JSON 이외의 설명이나 마크다운 코드 블록을 출력하지 않는다.
            JSON 형식:
            {"problems":[{"title":"","description":"","requirements":[""],
            "inputExample":"","outputExample":"","starterCode":"class Solution { ... }","difficulty":"쉬움|보통|어려움",
            "tests":[{"name":"기본 케이스","input":"입력값","expected":"기대 출력"}]}]}
            탐지 목록: %s
            참고할 Java 코드: %s
            """.formatted(objectMapper.valueToTree(selected), code);
        try {
            JsonNode root = readGeminiJson(callGemini(prompt));
            JsonNode problems = root.isArray() ? root : root.path("problems");
            if (!problems.isArray() || problems.size() != 3) {
                throw new IllegalStateException("AI가 코딩 문제를 정확히 3개 생성하지 못했습니다. 다시 시도해 주세요.");
            }

            List<CodingProblemDraft> result = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                String grammar = selected.get(i).name();
                JsonNode node = problems.get(i);
                List<String> requirements = new ArrayList<>(); node.path("requirements").forEach(item -> requirements.add(item.asText()));
                requirements.removeIf(String::isBlank);
                if (requirements.isEmpty()) requirements.add(grammar + " 문법을 활용해 요구 기능을 구현하세요.");

                JsonNode testNodes = node.path("tests");
                if (!testNodes.isArray()) testNodes = node.path("testCases");
                if (!testNodes.isArray() || testNodes.size() != 3) {
                    throw new IllegalStateException(grammar + " 문제의 테스트 케이스가 정확히 3개가 아닙니다.");
                }
                String inputExample = firstNonBlank(jsonValueText(node.path("inputExample")), jsonValueText(testNodes.get(0).path("input")));
                String outputExample = firstNonBlank(jsonValueText(node.path("outputExample")), jsonValueText(testNodes.get(0).path("expected")));
                List<CodingProblemDraft.TestCase> tests = new ArrayList<>();
                for (int testIndex = 0; testIndex < 3; testIndex++) {
                    JsonNode test = testNodes.get(testIndex);
                    tests.add(new CodingProblemDraft.TestCase(testIndex + 1,
                        firstNonBlank(test.path("name").asText(), "테스트 케이스 " + (testIndex + 1)),
                        requiredValue(firstNonBlank(jsonValueText(test.path("input")), jsonValueText(test.path("inputValue")), inputExample), "테스트 입력"),
                        requiredValue(firstNonBlank(jsonValueText(test.path("expected")), jsonValueText(test.path("expectedOutput")), jsonValueText(test.path("output")), outputExample), "테스트 기대 출력")));
                }
                result.add(new CodingProblemDraft(grammar, requiredText(node, "title", "제목"),
                    requiredText(node, "description", "설명"), requirements,
                    requiredValue(inputExample, "입력 예시"), requiredValue(outputExample, "출력 예시"),
                    requiredText(node, "starterCode", "시작 코드"),
                    node.path("difficulty").asText("보통"), tests));
            }
            return result;
        } catch (Exception e) {
            log.warn("Gemini 코딩 문제 생성 응답 검증 실패: {}", e.getMessage());
            if (e instanceof IllegalStateException state) throw state;
            throw new IllegalStateException("Gemini 코딩 문제 결과를 처리하지 못했습니다.", e);
        }
    }

    private String requiredText(JsonNode node, String field, String label) {
        String value = node.path(field).asText().trim();
        if (value.isEmpty()) throw new IllegalStateException("AI가 생성한 " + label + "이 비어 있습니다. 다시 시도해 주세요.");
        return value;
    }

    private String requiredValue(String value, String label) {
        if (value.isBlank()) throw new IllegalStateException("AI가 생성한 " + label + "이 비어 있습니다. 다시 시도해 주세요.");
        return value;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) return value.trim();
        }
        return "";
    }

    private String jsonValueText(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) return "";
        return node.isTextual() ? node.asText().trim() : node.toString();
    }

    public CodingReviewResponse reviewSolution(CodingProblemDraft problem, String sourceCode) {
        String prompt = """
            실제 코드를 실행하지 말고 Java 소스의 논리만 검토하라. 아래 문제와 테스트케이스 3개에 대해 예상 통과 여부를 판단한다.
            컴파일 불가능, TODO 유지, 핵심 문법 미사용, 요구사항 누락은 실패로 판단한다.
            정답 코드를 직접 제공하지 말고 실패 원인과 다음 수정 방향을 한국어로 설명한다.
            hint는 300자 이내, improvement는 800자 이내, 각 테스트의 reason은 300자 이내로 핵심만 작성한다.
            JSON 형식: {"status":"passed|failed","hint":"","improvement":"",
            "tests":[{"id":1,"name":"","status":"passed|failed","input":"","expected":"","actual":"AI 예상 결과","reason":"판단 근거"}]}
            문제: %s
            제출 코드: %s
            """.formatted(objectMapper.valueToTree(problem), sourceCode);
        try {
            JsonNode root = readGeminiJson(callGemini(prompt)); List<CodingReviewResponse.TestResult> tests = new ArrayList<>();
            root.path("tests").forEach(test -> tests.add(new CodingReviewResponse.TestResult(test.path("id").asInt(), test.path("name").asText(),
                test.path("status").asText(), test.path("input").asText(), test.path("expected").asText(), test.path("actual").asText(), test.path("reason").asText())));
            if (tests.size() != 3) throw new IllegalStateException("AI 검토 결과에 테스트 3개가 필요합니다.");
            boolean allPassed = tests.stream().allMatch(test -> "passed".equals(test.status()));
            return new CodingReviewResponse(allPassed ? "passed" : "failed", root.path("hint").asText(), root.path("improvement").asText(), tests);
        } catch (Exception e) {
            if (e instanceof IllegalStateException state) throw state;
            throw new IllegalStateException("Gemini 코드 검토 결과를 처리하지 못했습니다.", e);
        }
    }
}
