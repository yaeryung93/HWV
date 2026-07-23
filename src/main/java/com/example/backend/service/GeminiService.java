package com.example.backend.service;

import com.example.backend.dto.JavaAnalysisResponse;
import com.example.backend.dto.CodingProblemDraft;
import com.example.backend.dto.CodingProblemTranslationDraft;
import com.example.backend.dto.CodingReviewResponse;
import com.example.backend.dto.GeneratedLearningContent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
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
    private final String model;

    public GeminiService(JavaSyntaxDetector syntaxDetector,
                         @Value("${gemini.api.model:gemini-2.5-flash}") String model) {
        this.syntaxDetector = syntaxDetector;
        this.model = model == null || model.isBlank() ? "gemini-2.5-flash" : model.trim();
    }

    private String callGemini(String prompt) {
        Map<String, Object> body = Map.of(
            "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))),
            "generationConfig", Map.of("responseMimeType", "application/json", "temperature", 0.2, "maxOutputTokens", 16384)
        );
        Map<?, ?> response = webClient.post()
            .uri("/v1beta/models/{model}:generateContent", model)
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

    public GeneratedLearningContent generateAll(String code, String requestedDifficulty, String language) {
        List<JavaSyntaxDetector.Detected> detected = syntaxDetector.detect(code);
        List<JavaSyntaxDetector.Detected> selected = detected.subList(0, 3);
        String difficulty = normalizeDifficulty(requestedDifficulty);
        String responseLanguage = responseLanguage(language);
        String prompt = """
            summary, grammars, codingProblems의 기본 설명과 제목은 반드시 %s로 작성하라.
            단, translations의 ko, en, ja 항목은 각 키에 지정된 언어로 작성하라.
            Java 코드, 클래스명, 메서드명, 변수명, JSON 필드명과 테스트 값은 번역하지 마라.
            다음 Java 코드에 대한 학습 콘텐츠를 한 번에 생성하라.
            서버가 실제 코드에서 탐지한 문법 3개만 사용하고 목록 밖의 Java 일반 지식은 추가하지 않는다.

            1. summary: 코드의 목적과 흐름을 500자 이내로 요약한다.
            2. grammars: 탐지 목록 순서대로 정확히 3개를 설명한다. name과 evidence는 탐지 목록 값을 그대로 사용한다.
            3. codingProblems: 탐지 목록 순서대로 문법마다 프로그래머스 형식의 Java 코딩 문제를 하나씩, 정확히 3개 만든다.
            객관식이 아닌 구현 문제이며 title은 80자, description은 1,000자 이내로 작성한다.
            각 코딩 문제에는 서로 다른 테스트를 정확히 3개 넣고 input과 expected를 짧은 문자열로 작성한다.
            프로그래머스 방식으로 사용자는 Solution 클래스의 solution 메서드만 구현하고 반환한다.
            methodName은 항상 solution이다. returnType과 parameterTypes는 실제 starterCode 선언과 정확히 일치해야 한다.
            지원 타입은 int, long, double, boolean, String과 이 타입들의 1차원 또는 2차원 배열만 사용한다.
            입력값의 형태와 parameterTypes를 반드시 일치시킨다. 숫자, boolean, JSON 배열을 편의상 String으로 선언하지 않는다.
            2차원 배열을 사용하는 문제는 int[][], long[][], double[][], boolean[][], String[][] 중 실제 값에 맞는 타입을 사용한다.
            예: 15는 int, 3000000000은 long, 1.5는 double, true는 boolean, [1,2,3]은 int[], [1.5,2.5]는 double[], ["a","b"]는 String[]이다.
            실제 문자열 입력일 때만 String을 사용하며 사용자가 solution 안에서 숫자나 배열을 직접 파싱하게 만들지 않는다.
            starterCode는 public class Solution과 public solution 메서드를 포함하고 컴파일 가능한 기본 return 값을 넣되 정답 로직은 TODO로 남긴다.
            starterCode는 반드시 4칸 들여쓰기와 줄바꿈을 적용한다. 클래스 선언, 메서드 선언, TODO, return, 닫는 중괄호를 각각 알맞은 줄에 작성한다.
            starterCode 안에 마크다운 코드 블록(```java, ```)을 넣지 말고 순수 Java 코드만 작성한다.
            main 메서드, Scanner, System.in, System.out은 starterCode에 넣지 않는다. 서버가 숨겨진 실행 코드를 자동으로 붙인다.
            각 테스트의 arguments는 parameterTypes 순서와 개수가 같은 문자열 배열이다. 숫자와 boolean은 평문, String은 따옴표 없는 값, 배열은 JSON 배열 문자열로 작성한다.
            input은 화면 표시용 입력, expected는 solution의 기대 반환값을 문자열로 작성한다.
            난이도는 문제 설명의 길이가 아니라 해결에 필요한 알고리즘적 사고, 조건 조합, 반복 구조와 효율성을 기준으로 판단한다.
            쉬움은 프로그래머스 Lv.0~Lv.1 수준이다. 단순 조건문, 반복문, 배열 순회로 해결하며 한 번의 순회로 풀 수 있고 복잡한 알고리즘은 요구하지 않는다.
            보통은 프로그래머스 Lv.1~Lv.2 수준이다. 여러 조건의 조합, 중첩 반복, 문자열·배열 가공, 간단한 정렬·누적·빈도 계산을 요구할 수 있다.
            어려움은 프로그래머스 Lv.3~Lv.4 수준이다. 여러 단계의 알고리즘적 사고, 복잡한 예외 조건, 시간 복잡도와 효율성을 고려해야 한다.
            Lv.1 수준에서 쉬움은 직관적인 단일 순회 문제로, 보통은 조건 조합이나 추가 데이터 가공이 필요한 문제로 구분한다.
            난이도를 높이기 위해 탐지 목록에 없는 Java 문법을 억지로 요구해서는 안 되며, 업로드 코드에서 실제 탐지된 문법 안에서 문제의 사고 난이도를 조절한다.
            난이도 규칙은 반드시 지킨다: %s
            4. translations: codingProblems 3개의 자연어 표시 내용을 한국어(ko), 영어(en), 일본어(ja)로 모두 제공한다.
            ko, en, ja 배열은 각각 정확히 3개이며 codingProblems와 같은 순서여야 한다.
            각 번역에는 grammarName, title, description, summary, requirements, testNames만 포함한다.
            summary는 학습 목표를 설명하는 160자 이내의 한 문장으로 작성한다.
            번역 description은 600자 이내, requirements는 원본과 같은 개수이며 각 항목은 120자 이내로 작성한다.
            testNames는 해당 문제의 테스트 3개와 같은 순서로 정확히 3개를 작성한다.
            코드, 메서드명, 변수명, Java 타입, 입력값, 기대 출력값, 숫자는 번역하거나 translations에 중복하지 않는다.
            JSON 이외의 설명이나 마크다운 코드 블록은 출력하지 않는다.

            JSON 형식:
            {"summary":"","grammars":[{"name":"","description":"","rating":3,"evidence":""}],
            "codingProblems":[{"title":"","description":"","requirements":[""],"inputExample":"","outputExample":"",
            "methodName":"solution","returnType":"int","parameterTypes":["int"],
            "starterCode":"public class Solution {\\n    public int solution(int value) {\\n        // TODO\\n        return 0;\\n    }\\n}","difficulty":"쉬움|보통|어려움",
            "tests":[{"name":"기본 케이스","input":"5","arguments":["5"],"expected":"10"}]}],
            "translations":{"ko":[{"grammarName":"","title":"","description":"","summary":"","requirements":[""],"testNames":["", "", ""]}],
            "en":[{"grammarName":"","title":"","description":"","summary":"","requirements":[""],"testNames":["", "", ""]}],
            "ja":[{"grammarName":"","title":"","description":"","summary":"","requirements":[""],"testNames":["", "", ""]}]}}

            탐지 목록: %s
            Java 코드: %s
            """.formatted(responseLanguage, difficultyInstruction(difficulty), objectMapper.valueToTree(selected), code);
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

            List<CodingProblemDraft> codingProblems = parseCodingProblems(root.path("codingProblems"), selected, difficulty);
            Map<String, List<CodingProblemTranslationDraft>> translations =
                parseProblemTranslations(root.path("translations"), codingProblems);
            return new GeneratedLearningContent(analysis, codingProblems, translations);
        } catch (Exception e) {
            if (e instanceof WebClientResponseException webClientError) {
                log.warn("Gemini API 요청 실패: model={}, status={}, response={}",
                    model, webClientError.getStatusCode().value(), safeErrorResponse(webClientError));
                throw webClientError;
            }
            if (e instanceof IllegalStateException state) throw state;
            log.error("Gemini 통합 학습 결과 처리 실패: {}", e.getClass().getSimpleName(), e);
            throw new IllegalStateException("Gemini 통합 학습 결과를 처리하지 못했습니다.", e);
        }
    }

    private String responseLanguage(String language) {
        return switch (language == null ? "ko" : language.trim().toLowerCase()) {
            case "en" -> "English";
            case "ja" -> "Japanese";
            default -> "Korean";
        };
    }

    public CodingProblemTranslationDraft translateProblem(CodingProblemDraft problem, String language) {
        String targetLanguage = responseLanguage(language);
        String prompt = """
            Translate the natural-language content of this Java coding problem into %s.
            Return JSON only. Do not add Markdown fences or explanations.

            Translate only grammarName, title, description, summary, requirements, and testNames.
            Keep Java keywords, class names, method names, variable names, type names, numbers,
            input values, expected values, and code identifiers unchanged.
            Keep the exact number and order of requirements and test names.
            summary must be one short sentence describing the learning goal.

            JSON format:
            {"grammarName":"","title":"","description":"","summary":"",
             "requirements":[""],"testNames":[""]}

            Original problem:
            %s
            """.formatted(targetLanguage, objectMapper.valueToTree(problem));
        try {
            JsonNode root = readGeminiJson(callGemini(prompt));
            List<String> requirements = readTextArray(root.path("requirements"));
            List<String> testNames = readTextArray(root.path("testNames"));
            if (requirements.size() != problem.requirements().size()) {
                throw new IllegalStateException("번역된 제약 조건 수가 원본과 다릅니다.");
            }
            if (testNames.size() != problem.tests().size()) {
                throw new IllegalStateException("번역된 테스트 이름 수가 원본과 다릅니다.");
            }
            return new CodingProblemTranslationDraft(
                requiredText(root, "grammarName", "번역 문법명"),
                requiredText(root, "title", "번역 제목"),
                requiredText(root, "description", "번역 설명"),
                requiredText(root, "summary", "번역 요약"),
                requirements,
                testNames
            );
        } catch (Exception e) {
            if (e instanceof WebClientResponseException webClientError) {
                log.warn("Gemini 문제 번역 요청 실패: model={}, status={}, response={}",
                    model, webClientError.getStatusCode().value(), safeErrorResponse(webClientError));
                throw webClientError;
            }
            if (e instanceof IllegalStateException state) throw state;
            throw new IllegalStateException("문제 번역 결과를 처리하지 못했습니다.", e);
        }
    }

    private List<String> readTextArray(JsonNode node) {
        List<String> values = new ArrayList<>();
        if (node.isArray()) node.forEach(item -> values.add(item.asText()));
        return values;
    }

    private Map<String, List<CodingProblemTranslationDraft>> parseProblemTranslations(
            JsonNode translationsNode, List<CodingProblemDraft> problems) {
        Map<String, List<CodingProblemTranslationDraft>> translations = new LinkedHashMap<>();
        for (String language : List.of("ko", "en", "ja")) {
            JsonNode languageNodes = translationsNode.path(language);
            if (!languageNodes.isArray() || languageNodes.size() != problems.size()) {
                throw new IllegalStateException(language + " 문제 번역이 정확히 3개가 아닙니다.");
            }
            List<CodingProblemTranslationDraft> languageTranslations = new ArrayList<>();
            for (int index = 0; index < problems.size(); index++) {
                CodingProblemDraft problem = problems.get(index);
                JsonNode node = languageNodes.get(index);
                List<String> requirements = readTextArray(node.path("requirements"));
                List<String> testNames = readTextArray(node.path("testNames"));
                if (requirements.size() != problem.requirements().size()) {
                    throw new IllegalStateException(language + " 번역의 제약 조건 수가 원본과 다릅니다.");
                }
                if (testNames.size() != problem.tests().size()) {
                    throw new IllegalStateException(language + " 번역의 테스트 이름 수가 원본과 다릅니다.");
                }
                languageTranslations.add(new CodingProblemTranslationDraft(
                    requiredText(node, "grammarName", language + " 문법명"),
                    requiredText(node, "title", language + " 제목"),
                    requiredText(node, "description", language + " 설명"),
                    requiredText(node, "summary", language + " 요약"),
                    requirements,
                    testNames
                ));
            }
            translations.put(language, languageTranslations);
        }
        return translations;
    }

    private String safeErrorResponse(WebClientResponseException exception) {
        String response = exception.getResponseBodyAsString();
        if (response == null || response.isBlank()) return "응답 본문 없음";
        String sanitized = apiKey == null || apiKey.isBlank() ? response : response.replace(apiKey, "[API_KEY]");
        sanitized = sanitized.replaceAll("(?i)(api[_-]?key|key)\\s*[:=]\\s*[^,}\\s]+", "$1=[MASKED]")
            .replaceAll("[\\r\\n]+", " ")
            .trim();
        return sanitized.length() > 1000 ? sanitized.substring(0, 1000) + "..." : sanitized;
    }

    public String summarize(String text) {
        return callGemini("다음 내용을 한국어 JSON으로 요약해줘: " + text);
    }

    private List<CodingProblemDraft> parseCodingProblems(JsonNode problems, List<JavaSyntaxDetector.Detected> selected,
                                                          String difficulty) {
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
                String methodName = requiredText(node, "methodName", "메서드 이름");
                if (!"solution".equals(methodName)) throw new IllegalStateException("메서드 이름은 solution이어야 합니다.");
                String returnType = supportedType(requiredText(node, "returnType", "반환형"));
                List<String> parameterTypes = new ArrayList<>();
                for (JsonNode type : node.path("parameterTypes")) parameterTypes.add(supportedType(type.asText()));
                if (parameterTypes.isEmpty()) throw new IllegalStateException(grammar + " 문제의 매개변수 타입이 없습니다.");
                for (int testIndex = 0; testIndex < 3; testIndex++) {
                    JsonNode test = testNodes.get(testIndex);
                    List<String> arguments = new ArrayList<>();
                    test.path("arguments").forEach(argument -> arguments.add(jsonValueText(argument)));
                    if (arguments.size() != parameterTypes.size()) {
                        throw new IllegalStateException(grammar + " 문제의 테스트 인자 개수가 매개변수 개수와 다릅니다.");
                    }
                    tests.add(new CodingProblemDraft.TestCase(testIndex + 1,
                        firstNonBlank(test.path("name").asText(), "테스트 케이스 " + (testIndex + 1)),
                        requiredValue(firstNonBlank(jsonValueText(test.path("input")), jsonValueText(test.path("inputValue")), inputExample), "테스트 입력"),
                        requiredValue(firstNonBlank(jsonValueText(test.path("expected")), jsonValueText(test.path("expectedOutput")), jsonValueText(test.path("output")), outputExample), "테스트 기대 출력"),
                        arguments));
                }
                parameterTypes = inferParameterTypes(parameterTypes, tests);
                String starterCode = requiredText(node, "starterCode", "시작 코드");
                starterCode = alignStarterParameterTypes(starterCode, returnType, parameterTypes);
                starterCode = formatStarterCode(starterCode);
                if (!starterCode.matches("(?s).*\\bpublic\\s+class\\s+Solution\\b.*")) {
                    throw new IllegalStateException(grammar + " 문제의 시작 코드에 public class Solution이 없습니다.");
                }
                if (!starterCode.matches("(?s).*\\bpublic\\s+" + java.util.regex.Pattern.quote(returnType)
                    + "\\s+solution\\s*\\(.*")) {
                    throw new IllegalStateException(grammar + " 문제의 solution 선언과 반환형이 일치하지 않습니다.");
                }
                result.add(new CodingProblemDraft(grammar, requiredText(node, "title", "제목"),
                    requiredText(node, "description", "설명"), requirements,
                    requiredValue(inputExample, "입력 예시"), requiredValue(outputExample, "출력 예시"),
                    starterCode, difficultyForIndex(difficulty, i), methodName, returnType, parameterTypes, tests));
            }
            return result;
        } catch (Exception e) {
            log.warn("Gemini 코딩 문제 생성 응답 검증 실패: {}", e.getMessage());
            if (e instanceof IllegalStateException state) throw state;
            throw new IllegalStateException("Gemini 코딩 문제 결과를 처리하지 못했습니다.", e);
        }
    }

    private String supportedType(String type) {
        String normalized = type == null ? "" : type.replace(" ", "").trim();
        if (!Set.of("int", "long", "double", "boolean", "String",
            "int[]", "long[]", "double[]", "boolean[]", "String[]",
            "int[][]", "long[][]", "double[][]", "boolean[][]", "String[][]").contains(normalized)) {
            throw new IllegalStateException("지원하지 않는 solution 타입입니다: " + normalized);
        }
        return normalized;
    }

    private List<String> inferParameterTypes(List<String> declaredTypes, List<CodingProblemDraft.TestCase> tests) {
        List<String> inferred = new ArrayList<>();
        for (int index = 0; index < declaredTypes.size(); index++) {
            List<String> values = new ArrayList<>();
            for (CodingProblemDraft.TestCase test : tests) values.add(test.arguments().get(index));
            inferred.add(inferType(values, declaredTypes.get(index)));
        }
        return inferred;
    }

    private String inferType(List<String> values, String fallback) {
        try {
            List<JsonNode> nodes = values.stream().map(value -> {
                try { return objectMapper.readTree(value); }
                catch (Exception ignored) { return objectMapper.getNodeFactory().textNode(value); }
            }).toList();
            if (nodes.stream().allMatch(JsonNode::isArray)) {
                boolean twoDimensional = nodes.stream()
                    .flatMap(node -> java.util.stream.StreamSupport.stream(node.spliterator(), false))
                    .anyMatch(JsonNode::isArray);
                if (twoDimensional) {
                    List<JsonNode> elements = new ArrayList<>();
                    nodes.forEach(rows -> rows.forEach(row -> {
                        if (row.isArray()) row.forEach(elements::add);
                    }));
                    if (elements.isEmpty()) return fallback.endsWith("[][]") ? fallback : "int[][]";
                    if (elements.stream().allMatch(JsonNode::isBoolean)) return "boolean[][]";
                    if (elements.stream().allMatch(JsonNode::isIntegralNumber)) {
                        return elements.stream().allMatch(value -> value.canConvertToInt()) ? "int[][]" : "long[][]";
                    }
                    if (elements.stream().allMatch(JsonNode::isNumber)) return "double[][]";
                    return "String[][]";
                }
                List<JsonNode> elements = new ArrayList<>();
                nodes.forEach(array -> array.forEach(elements::add));
                if (elements.isEmpty()) return fallback.endsWith("[]") ? fallback : "int[]";
                if (elements.stream().allMatch(JsonNode::isBoolean)) return "boolean[]";
                if (elements.stream().allMatch(JsonNode::isIntegralNumber)) {
                    return elements.stream().allMatch(value -> value.canConvertToInt()) ? "int[]" : "long[]";
                }
                if (elements.stream().allMatch(JsonNode::isNumber)) return "double[]";
                return "String[]";
            }
            if (nodes.stream().allMatch(JsonNode::isBoolean)) return "boolean";
            if (nodes.stream().allMatch(JsonNode::isIntegralNumber)) {
                return nodes.stream().allMatch(value -> value.canConvertToInt()) ? "int" : "long";
            }
            if (nodes.stream().allMatch(JsonNode::isNumber)) return "double";
            return "String";
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private String alignStarterParameterTypes(String starterCode, String returnType, List<String> parameterTypes) {
        java.util.regex.Pattern signature = java.util.regex.Pattern.compile(
            "(?s)(\\bpublic\\s+" + java.util.regex.Pattern.quote(returnType) + "\\s+solution\\s*\\()([^)]*)(\\))");
        java.util.regex.Matcher matcher = signature.matcher(starterCode);
        if (!matcher.find()) return starterCode;
        String[] parameters = matcher.group(2).split(",", -1);
        if (parameters.length != parameterTypes.size()) return starterCode;
        List<String> aligned = new ArrayList<>();
        for (int index = 0; index < parameters.length; index++) {
            String parameter = parameters[index].trim();
            int space = parameter.lastIndexOf(' ');
            if (space < 0) return starterCode;
            aligned.add(parameterTypes.get(index) + " " + parameter.substring(space + 1).trim());
        }
        return matcher.replaceFirst(java.util.regex.Matcher.quoteReplacement(
            matcher.group(1) + String.join(", ", aligned) + matcher.group(3)));
    }

    private String formatStarterCode(String starterCode) {
        String decoded = starterCode.replace("\\r\\n", "\n").replace("\\n", "\n").replace("\\t", "    ").trim();
        if (decoded.contains("\n")) return decoded;

        String compact = decoded.replace("// TODO", "/* TODO */").replaceAll("\\s+", " ");
        StringBuilder formatted = new StringBuilder();
        StringBuilder line = new StringBuilder();
        int indent = 0;
        for (int index = 0; index < compact.length(); index++) {
            if (compact.startsWith("/* TODO */", index)) {
                appendStarterLine(formatted, line, indent);
                line.append("/* TODO */");
                appendStarterLine(formatted, line, indent);
                index += "/* TODO */".length() - 1;
                continue;
            }

            char current = compact.charAt(index);
            if (current == '{') {
                line.append('{');
                appendStarterLine(formatted, line, indent++);
            } else if (current == '}') {
                appendStarterLine(formatted, line, indent);
                indent = Math.max(0, indent - 1);
                line.append('}');
                appendStarterLine(formatted, line, indent);
            } else if (current == ';') {
                line.append(';');
                appendStarterLine(formatted, line, indent);
            } else {
                line.append(current);
            }
        }
        appendStarterLine(formatted, line, indent);
        return formatted.toString().trim();
    }

    private void appendStarterLine(StringBuilder formatted, StringBuilder line, int indent) {
        String value = line.toString().trim();
        line.setLength(0);
        if (value.isEmpty()) return;
        formatted.append("    ".repeat(Math.max(0, indent))).append(value).append('\n');
    }

    private String normalizeDifficulty(String difficulty) {
        return Set.of("쉬움", "보통", "어려움").contains(difficulty) ? difficulty : "균형";
    }

    private String difficultyInstruction(String difficulty) {
        return "균형".equals(difficulty)
            ? "첫 번째는 쉬움(프로그래머스 Lv.0~1), 두 번째는 보통(Lv.1~2), 세 번째는 어려움(Lv.3~4)으로 생성한다."
            : "세 문제 모두 " + difficulty + " 난이도로 생성한다. " + difficultyLevelRange(difficulty);
    }

    private String difficultyLevelRange(String difficulty) {
        return switch (difficulty) {
            case "쉬움" -> "프로그래머스 Lv.0~Lv.1 중 직관적인 단일 순회 수준을 지킨다.";
            case "보통" -> "프로그래머스 Lv.1~Lv.2 수준으로 조건 조합이나 데이터 가공을 포함한다.";
            case "어려움" -> "프로그래머스 Lv.3~Lv.4 수준으로 다단계 사고와 효율성 고려를 요구한다.";
            default -> "";
        };
    }

    private String difficultyForIndex(String difficulty, int index) {
        if (!"균형".equals(difficulty)) return difficulty;
        return List.of("쉬움", "보통", "어려움").get(index);
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
