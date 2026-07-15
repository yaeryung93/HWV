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
import java.util.*;

@Service
public class GeminiService {
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
        return objectMapper.readTree(cleaned);
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
            JsonNode root = objectMapper.readTree(callGemini(prompt));
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
            JsonNode quizzes = objectMapper.readTree(callGemini(prompt)).path("quizzes");
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
        String prompt = """
            아래 Java 코드에서 서버가 검증한 핵심 문법 3개 각각에 대해 프로그래머스 형식의 코딩 문제를 정확히 1개씩 생성하라.
            따라서 문제는 정확히 3개이며, 각 grammarName은 탐지 목록의 이름을 한 번씩 그대로 사용한다.
            원본 코드를 복사하거나 원본 변수명을 묻지 말고, 해당 문법을 실제로 활용해야 해결 가능한 새로운 문제를 만든다.
            각 문제에는 서로 다른 입력을 가진 테스트케이스를 정확히 3개 만든다.
            실제 실행기는 없으므로 입력과 기대 출력은 AI 논리 검토가 가능한 간결한 문자열로 작성한다.
            JSON 형식:
            {"problems":[{"grammarName":"","title":"","description":"","requirements":[""],
            "inputExample":"","outputExample":"","starterCode":"class Solution { ... }","difficulty":"쉬움|보통|어려움",
            "tests":[{"id":1,"name":"기본 케이스","input":"","expected":""}]}]}
            탐지 목록: %s
            참고할 Java 코드: %s
            """.formatted(objectMapper.valueToTree(detected), code);
        try {
            JsonNode root = readGeminiJson(callGemini(prompt));
            JsonNode problems = root.isArray() ? root : root.path("problems");
            if (problems.size() < 3) throw new IllegalStateException("AI가 코딩 문제 3개를 완성하지 못했습니다. 다시 시도해 주세요.");
            List<CodingProblemDraft> result = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                JsonNode node = problems.get(i);
                String grammar = detected.get(i).name();
                List<String> requirements = new ArrayList<>(); node.path("requirements").forEach(item -> requirements.add(item.asText()));
                if (requirements.isEmpty()) requirements.add(grammar + " 문법을 활용해 요구 기능을 구현하세요.");
                List<CodingProblemDraft.TestCase> tests = new ArrayList<>();
                for (int testIndex = 0; testIndex < Math.min(3, node.path("tests").size()); testIndex++) {
                    JsonNode test = node.path("tests").get(testIndex);
                    tests.add(new CodingProblemDraft.TestCase(testIndex + 1,
                        test.path("name").asText("테스트 케이스 " + (testIndex + 1)), test.path("input").asText(), test.path("expected").asText()));
                }
                while (tests.size() < 3) {
                    int testNumber = tests.size() + 1;
                    tests.add(new CodingProblemDraft.TestCase(testNumber, "추가 검증 " + testNumber,
                        testNumber == 1 ? node.path("inputExample").asText() : "경계값 입력 " + testNumber,
                        testNumber == 1 ? node.path("outputExample").asText() : "문제 요구사항을 만족하는 결과"));
                }
                result.add(new CodingProblemDraft(grammar, node.path("title").asText(), node.path("description").asText(), requirements,
                    node.path("inputExample").asText(), node.path("outputExample").asText(), node.path("starterCode").asText(),
                    node.path("difficulty").asText("보통"), tests));
            }
            return result;
        } catch (Exception e) {
            if (e instanceof IllegalStateException state) throw state;
            throw new IllegalStateException("Gemini 코딩 문제 결과를 처리하지 못했습니다.", e);
        }
    }

    public CodingReviewResponse reviewSolution(CodingProblemDraft problem, String sourceCode) {
        String prompt = """
            실제 코드를 실행하지 말고 Java 소스의 논리만 검토하라. 아래 문제와 테스트케이스 3개에 대해 예상 통과 여부를 판단한다.
            컴파일 불가능, TODO 유지, 핵심 문법 미사용, 요구사항 누락은 실패로 판단한다.
            정답 코드를 직접 제공하지 말고 실패 원인과 다음 수정 방향을 한국어로 설명한다.
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
