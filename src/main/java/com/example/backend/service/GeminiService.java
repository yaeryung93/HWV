package com.example.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import com.example.backend.entity.Quiz;
import java.util.ArrayList;
import java.util.List;

import java.util.Map;

@Service
public class GeminiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    private final WebClient webClient = WebClient.builder()
            .baseUrl("https://generativelanguage.googleapis.com")
            .build();

    private String callGemini(String prompt) {

        try {

            Map<String, Object> body = Map.of(
                    "contents", new Object[]{
                            Map.of(
                                    "parts", new Object[]{
                                            Map.of("text", prompt)
                                    }
                            )
                    }
            );

            Map response = webClient.post()
                    .uri("/v1beta/models/gemini-flash-latest:generateContent")
                    .header("X-goog-api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            System.out.println(response);

            var candidates = (List<?>) response.get("candidates");

            if (candidates == null || candidates.isEmpty()) {
                return "Gemini 응답에 candidates가 없습니다.\n" + response;
            }

            var candidate = (Map<?, ?>) candidates.get(0);
            var content = (Map<?, ?>) candidate.get("content");
            var parts = (List<?>) content.get("parts");
            var part = (Map<?, ?>) parts.get(0);

            return part.get("text").toString();

        } catch (Exception e) {

            e.printStackTrace();

            return e.getMessage();
        }
    }

    public String analyzeCode(String code) {

        String prompt = """
            다음 Java 코드를 분석해.

            다음 형식(JSON)으로만 응답해.

            {
                "language":"Java",
                "grammars":[
                    {
                    "name":"",
                    "description":"",
                    "importance":5
                    }
                ]
            }

            코드:

            """ + code;

        return callGemini(prompt);
    }

    public String analyzeAndGenerate(String code) {

        String prompt = """
            다음 Java 코드를 분석해.
            
            해야 할 일
            
            1. 핵심 Java 문법 3개 추출
            2. 각각 설명
            3. 중요도(1~5)
            4. 그 문법을 기반으로 객관식 문제 5개 생성
            
            JSON만 출력.
            
            형식
            
            {
              "grammars":[
                {
                  "name":"",
                  "description":"",
                  "importance":5
                }
              ],
              "quizzes":[
                {
                  "question":"",
                  "option1":"",
                  "option2":"",
                  "option3":"",
                  "option4":"",
                  "option5":"",
                  "answer":1,
                  "explanation":""
                }
              ]
            }
            
            코드:
            
            """ + code;

        return callGemini(prompt);
    }

    public List<Quiz> generateQuiz(String summary) {
        return new ArrayList<>();
    }

    public String summarize(String text) {

        String prompt = """
        다음 내용을 핵심만 한국어로 요약해줘.

        """ + text;

        return callGemini(prompt);
    }
}
