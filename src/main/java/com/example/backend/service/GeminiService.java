package com.example.backend.service;

import com.example.backend.dto.JavaAnalysisResponse;
import com.example.backend.dto.CodingProblemDraft;
import com.example.backend.dto.CodingReviewResponse;
import com.example.backend.dto.GeneratedLearningContent;
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
            "generationConfig", Map.of("responseMimeType", "application/json", "temperature", 0.2, "maxOutputTokens", 8192)
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

    public GeneratedLearningContent generateAll(String code) {
        List<JavaSyntaxDetector.Detected> detected = syntaxDetector.detect(code);
        List<JavaSyntaxDetector.Detected> selected = detected.subList(0, 3);
        String prompt = """
            다음 Java 코드에 대한 학습 콘텐츠를 한 번에 생성하라.
            서버가 실제 코드에서 탐지한 문법 3개만 사용하고 목록 밖의 Java 일반 지식은 추가하지 않는다.

            1. summary: 코드의 목적과 흐름을 500자 이내로 요약한다.
            2. grammars: 탐지 목록 순서대로 정확히 3개를 설명한다. name과 evidence는 탐지 목록 값을 그대로 사용한다.
            3. quizzes: 탐지 목록 순서대로 문법마다 실제 코드 표현을 근거로 하는 객관식 문제를 하나씩, 정확히 3개 만든다. answer는 1부터 5까지다.
            4. codingProblems: 탐지 목록 순서대로 문법마다 프로그래머스 형식의 Java 코딩 문제를 하나씩, 정확히 3개 만든다.
            객관식이 아닌 구현 문제이며 title은 80자, description은 1,000자 이내로 작성한다.
            각 코딩 문제에는 서로 다른 테스트를 정확히 3개 넣고 input과 expected를 짧은 문자열로 작성한다.
            starterCode에는 class Solution과 구현할 메서드 선언만 넣고 정답은 넣지 않는다.
            JSON 이외의 설명이나 마크다운 코드 블록은 출력하지 않는다.

            JSON 형식:
            {"summary":"","grammars":[{"name":"","description":"","rating":3,"evidence":""}],
            "quizzes":[{"grammarName":"","question":"","option1":"","option2":"","option3":"","option4":"","option5":"","answer":1,"explanation":""}],
            "codingProblems":[{"title":"","description":"","requirements":[""],"inputExample":"","outputExample":"",
            "starterCode":"class Solution { ... }","difficulty":"쉬움|보통|어려움",
            "tests":[{"name":"기본 케이스","input":"입력값","expected":"기대 출력"}]}]}

            탐지 목록: %s
            Java 코드: %s
            """.formatted(objectMapper.valueToTree(selected), code);
        try {
            JsonNode root = readGeminiJson(callGemini(prompt));
            List<JavaAnalysisResponse.Grammar> grammars = new ArrayList<>();
            JsonNode grammarNodes = root.path("grammars");
            for (int i = 0; i < selected.size(); i++) {
                JavaSyntaxDetector.Detected item = selected.get(i);
                JsonNode node = i < grammarNodes.size() ? grammarNodes.get(i) : null;
                String description = node == null ? item.name() + " 문법이 업로드한 코드에서 사용되었습니다." : node.path("description").asText();
                int rating = node == null ? 3 : Math.max(1, Math.min(5, node.path("rating").asInt(3)));
                grammars.add(new JavaAnalysisResponse.Grammar(item.name(), description, rating, item.evidence()));
            }
            JavaAnalysisResponse analysis = new JavaAnalysisResponse(
                root.path("summary").asText("업로드한 Java 코드를 분석했습니다."), grammars, code);

            JsonNode quizzes = root.path("quizzes");
            if (quizzes.size() != 3) throw new IllegalStateException("Gemini가 객관식 문제 3개를 반환하지 않았습니다.");
            List<Quiz> quizResult = new ArrayList<>();
            Set<String> allowedGrammarNames = new LinkedHashSet<>();
            selected.forEach(item -> allowedGrammarNames.add(item.name()));
            for (int i = 0; i < quizzes.size(); i++) {
                JsonNode node = quizzes.get(i);
                String grammarName = node.path("grammarName").asText();
                if (!allowedGrammarNames.contains(grammarName)) {
                    grammarName = selected.get(i).name();
                }
                int answer = node.path("answer").asInt();
                if (answer < 1 || answer > 5) throw new IllegalStateException("정답 번호가 올바르지 않습니다.");
                Quiz quiz = new Quiz();
                quiz.setGrammarName(grammarName); quiz.setQuestion(node.path("question").asText());
                quiz.setOption1(node.path("option1").asText()); quiz.setOption2(node.path("option2").asText());
                quiz.setOption3(node.path("option3").asText()); quiz.setOption4(node.path("option4").asText()); quiz.setOption5(node.path("option5").asText());
                quiz.setAnswer(answer); quiz.setExplanation(node.path("explanation").asText()); quizResult.add(quiz);
            }

            List<CodingProblemDraft> codingProblems = parseCodingProblems(root.path("codingProblems"), selected);
            return new GeneratedLearningContent(analysis, quizResult, codingProblems);
        } catch (Exception e) {
            if (e instanceof IllegalStateException state) throw state;
            throw new IllegalStateException("Gemini 통합 학습 결과를 처리하지 못했습니다.", e);
        }
    }

    public String summarize(String text) {
        return callGemini("다음 내용을 한국어 JSON으로 요약해줘: " + text);
    }

    private List<CodingProblemDraft> parseCodingProblems(JsonNode problems, List<JavaSyntaxDetector.Detected> selected) {
        try {
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
