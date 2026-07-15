package com.example.backend.service;

import com.example.backend.dto.JavaAnalysisResponse;
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

    public String analyzeAndGenerate(String code) { return callGemini(code); }
    public String summarize(String text) { return callGemini("다음 내용을 한국어 JSON으로 요약해줘: " + text); }
}
