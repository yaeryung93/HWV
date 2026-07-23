package com.example.backend.service;

import com.example.backend.dto.*;
import com.example.backend.entity.*;
import com.example.backend.repository.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
public class ProblemGenerationService {
    private final JavaCodeExecutionService executionService;
    private final CodingProblemRepository problemRepository;
    private final CodingSubmissionRepository submissionRepository;
    private final CodingProblemTranslationRepository translationRepository;
    private final UserRepository userRepository;
    private final GeminiService geminiService;
    private final StudyStreakService studyStreakService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ProblemGenerationService(JavaCodeExecutionService executionService, CodingProblemRepository problemRepository,
                                    CodingSubmissionRepository submissionRepository,
                                    CodingProblemTranslationRepository translationRepository,
                                    UserRepository userRepository, GeminiService geminiService,
                                    StudyStreakService studyStreakService) {
        this.executionService = executionService; this.problemRepository = problemRepository;
        this.submissionRepository = submissionRepository; this.translationRepository = translationRepository;
        this.userRepository = userRepository; this.geminiService = geminiService;
        this.studyStreakService = studyStreakService;
    }

    @Transactional
    public List<Map<String, Object>> generate(CodingProblemRequest request) {
        return findAll(request.getUserId(), "ko").stream().limit(3).toList();
    }

    @Transactional
    public List<Map<String, Object>> findAll(Long userId, String language) {
        User user = user(userId); Set<Long> solved = new HashSet<>();
        submissionRepository.findByUserOrderBySubmittedAtDesc(user).stream().filter(CodingSubmission::isPassed)
            .forEach(item -> solved.add(item.getProblem().getId()));
        List<CodingProblem> problems = problemRepository.findByUserOrderByCreatedAtDesc(user);
        String targetLanguage = normalizeLanguage(language);
        Map<Long, CodingProblemTranslation> translations = new HashMap<>();
        if (!problems.isEmpty()) {
            translationRepository.findByProblemIdInAndLanguage(
                problems.stream().map(CodingProblem::getId).toList(), targetLanguage
            ).forEach(item -> translations.put(item.getProblem().getId(), item));
        }
        List<CodingProblem> missingTranslations = problems.stream()
            .filter(problem -> !targetLanguage.equals(detectLanguage(problem)))
            .filter(problem -> !translations.containsKey(problem.getId()))
            .toList();
        if (!missingTranslations.isEmpty()) {
            List<CodingProblemTranslationDraft> drafts = geminiService.translateProblems(
                missingTranslations.stream().map(this::toDraft).toList(), targetLanguage);
            List<CodingProblemTranslation> saved = new ArrayList<>();
            for (int index = 0; index < missingTranslations.size(); index++) {
                saved.add(toTranslation(missingTranslations.get(index), targetLanguage, drafts.get(index)));
            }
            translationRepository.saveAll(saved)
                .forEach(item -> translations.put(item.getProblem().getId(), item));
        }
        return problems.stream().map(problem -> response(problem, solved.contains(problem.getId()),
            translations.get(problem.getId()), targetLanguage)).toList();
    }

    @Transactional
    public Map<String, Object> findOne(Long id, Long userId, String language) {
        CodingProblem problem = problemRepository.findByIdAndUser(id, user(userId))
            .orElseThrow(() -> new IllegalArgumentException("문제를 찾을 수 없습니다."));
        String targetLanguage = normalizeLanguage(language);
        CodingProblemTranslation translation = null;
        if (!targetLanguage.equals(detectLanguage(problem))) {
            translation = translationRepository.findByProblemAndLanguage(problem, targetLanguage)
                .orElseGet(() -> createTranslation(problem, targetLanguage));
        }
        return response(problem, false, translation, targetLanguage);
    }

    @Transactional
    public Map<String, Object> review(CodingSubmissionRequest request) {
        User user = user(request.getUserId());
        CodingProblem problem = problemRepository.findByIdAndUser(request.getProblemId(), user).orElseThrow(() -> new IllegalArgumentException("문제를 찾을 수 없습니다."));
        CodingReviewResponse review = executionService.execute(toDraft(problem), request.getSourceCode());
        int passedCount = (int) review.tests().stream().filter(test -> "passed".equals(test.status())).count();
        CodingSubmission submission = new CodingSubmission(); submission.setUser(user); submission.setProblem(problem);
        submission.setSourceCode(request.getSourceCode()); submission.setTestsJson(write(review.tests())); submission.setHint(review.hint());
        submission.setImprovement(review.improvement()); submission.setPassedCount(passedCount); submission.setTotalCount(3); submission.setPassed(passedCount == 3);
        submissionRepository.save(submission);
        studyStreakService.recordSubmission(user.getId(), submission.isPassed());
        Map<String, Object> result = new LinkedHashMap<>(); result.put("status", submission.isPassed() ? "passed" : "failed");
        result.put("hint", review.hint()); result.put("improvement", review.improvement()); result.put("tests", review.tests());
        result.put("attempt", submissionMap(submission)); return result;
    }

    public Map<String, Object> submissionMap(CodingSubmission item) {
        Map<String, Object> map = new LinkedHashMap<>(); map.put("id", item.getId()); map.put("problemId", item.getProblem().getId());
        map.put("problemTitle", item.getProblem().getTitle()); map.put("grammarName", item.getProblem().getGrammarName());
        map.put("passedCount", item.getPassedCount()); map.put("totalCount", item.getTotalCount()); map.put("passed", item.isPassed());
        map.put("hint", item.getHint()); map.put("improvement", item.getImprovement()); map.put("submittedAt", item.getSubmittedAt().toString());
        return map;
    }

    private CodingProblemDraft toDraft(CodingProblem problem) {
        return new CodingProblemDraft(problem.getGrammarName(), problem.getTitle(), problem.getDescription(), readStrings(problem.getRequirementsJson()),
            problem.getInputExample(), problem.getOutputExample(), problem.getStarterCode(), problem.getDifficulty(),
            problem.getMethodName(), problem.getReturnType(), readStrings(problem.getParameterTypesJson()), readTests(problem.getTestsJson()));
    }

    private Map<String, Object> response(CodingProblem problem, boolean solved,
                                         CodingProblemTranslation translation, String language) {
        String grammarName = translation == null ? problem.getGrammarName() : translation.getGrammarName();
        String title = translation == null ? problem.getTitle() : translation.getTitle();
        String description = translation == null ? problem.getDescription() : translation.getDescription();
        List<String> requirements = translation == null ? readStrings(problem.getRequirementsJson())
            : readStrings(translation.getRequirementsJson());
        List<String> translatedTestNames = translation == null ? List.of() : readStrings(translation.getTestNamesJson());
        List<CodingProblemDraft.TestCase> tests = readTests(problem.getTestsJson());

        Map<String, Object> map = new LinkedHashMap<>(); map.put("id", problem.getId()); map.put("title", title);
        map.put("category", grammarName); map.put("grammarName", grammarName); map.put("difficulty", problem.getDifficulty());
        map.put("description", description); map.put("summary", translation == null
            ? defaultSummary(grammarName, language) : translation.getSummary());
        map.put("requirements", requirements); map.put("inputExample", problem.getInputExample());
        map.put("outputExample", problem.getOutputExample()); map.put("starterCode", starterCode(problem)); map.put("language", "Java");
        map.put("methodName", problem.getMethodName()); map.put("returnType", problem.getReturnType());
        map.put("parameterTypes", readStrings(problem.getParameterTypesJson()));
        List<Map<String, Object>> responseTests = new ArrayList<>();
        for (int index = 0; index < tests.size(); index++) {
            CodingProblemDraft.TestCase test = tests.get(index);
            String testName = index < translatedTestNames.size() ? translatedTestNames.get(index) : test.name();
            responseTests.add(Map.of("id", test.id(), "name", testName, "input", test.input(),
                "expected", test.expected(), "status", "pending"));
        }
        map.put("tests", responseTests);
        map.put("contentLanguage", translation == null ? detectLanguage(problem) : language);
        map.put("solved", solved); map.put("progress", solved ? 100 : 0); return map;
    }

    private CodingProblemTranslation createTranslation(CodingProblem problem, String language) {
        CodingProblemTranslationDraft draft = geminiService.translateProblem(toDraft(problem), language);
        return translationRepository.save(toTranslation(problem, language, draft));
    }

    private CodingProblemTranslation toTranslation(CodingProblem problem, String language,
                                                     CodingProblemTranslationDraft draft) {
        CodingProblemTranslation translation = new CodingProblemTranslation();
        translation.setProblem(problem); translation.setLanguage(language);
        translation.setGrammarName(draft.grammarName()); translation.setTitle(draft.title());
        translation.setDescription(draft.description()); translation.setSummary(draft.summary());
        translation.setRequirementsJson(write(draft.requirements()));
        translation.setTestNamesJson(write(draft.testNames()));
        return translation;
    }

    private String normalizeLanguage(String language) {
        if (language == null) return "ko";
        return switch (language.trim().toLowerCase(Locale.ROOT)) {
            case "en" -> "en";
            case "ja" -> "ja";
            default -> "ko";
        };
    }

    private String detectLanguage(CodingProblem problem) {
        String content = String.join(" ", problem.getGrammarName(), problem.getTitle(), problem.getDescription(),
            String.join(" ", readStrings(problem.getRequirementsJson())),
            readTests(problem.getTestsJson()).stream().map(CodingProblemDraft.TestCase::name)
                .reduce("", (left, right) -> left + " " + right));
        if (content.matches("(?s).*[\\u3040-\\u30ff].*")) return "ja";
        if (content.matches("(?s).*[\\uac00-\\ud7a3].*")) return "ko";
        return "en";
    }

    private String defaultSummary(String grammarName, String language) {
        return switch (language) {
            case "en" -> "This AI coding problem uses " + grammarName + ".";
            case "ja" -> grammarName + "の文法を活用するAIコーディング問題です。";
            default -> grammarName + " 문법을 활용하는 AI 코딩 문제입니다.";
        };
    }

    private String write(Object value) { try { return objectMapper.writeValueAsString(value); } catch (Exception e) { throw new IllegalStateException("문제 데이터를 저장하지 못했습니다.", e); } }
    private String starterCode(CodingProblem problem) {
        String starterCode = problem.getStarterCode();
        if (problem.getMethodName() != null && starterCode != null
            && starterCode.matches("(?s).*\\bpublic\\s+class\\s+Solution\\b.*")) return formatStarterCode(starterCode);
        if (starterCode != null && starterCode.matches("(?s).*\\bpublic\\s+class\\s+Main\\b.*")) return formatStarterCode(starterCode);
        return """
            public class Main {
                public static String solution(String input) {
                    // TODO: input을 문제 조건에 맞게 처리하세요.
                    return "";
                }

                public static void main(String[] args) throws Exception {
                    String input = new String(System.in.readAllBytes()).trim();
                    System.out.print(solution(input));
                }
            }
            """;
    }
    private String formatStarterCode(String starterCode) {
        String decoded = starterCode.replace("\\r\\n", "\n").replace("\\n", "\n").replace("\\t", "    ").trim();
        if (decoded.contains("\n")) return decoded;

        String compact = decoded.replace("// TODO", "/* TODO */").replaceAll("\\s+", " "); StringBuilder formatted = new StringBuilder();
        StringBuilder line = new StringBuilder(); int indent = 0;
        for (int index = 0; index < compact.length(); index++) {
            if (compact.startsWith("/* TODO */", index)) {
                appendStarterLine(formatted, line, indent); line.append("/* TODO */");
                appendStarterLine(formatted, line, indent); index += "/* TODO */".length() - 1; continue;
            }
            char current = compact.charAt(index);
            if (current == '{') { line.append('{'); appendStarterLine(formatted, line, indent++); }
            else if (current == '}') { appendStarterLine(formatted, line, indent); indent = Math.max(0, indent - 1); line.append('}'); appendStarterLine(formatted, line, indent); }
            else if (current == ';') { line.append(';'); appendStarterLine(formatted, line, indent); }
            else line.append(current);
        }
        appendStarterLine(formatted, line, indent); return formatted.toString().trim();
    }
    private void appendStarterLine(StringBuilder formatted, StringBuilder line, int indent) {
        String value = line.toString().trim(); line.setLength(0); if (value.isEmpty()) return;
        formatted.append("    ".repeat(Math.max(0, indent))).append(value).append('\n');
    }
    private List<String> readStrings(String json) { try { return objectMapper.readValue(json, new TypeReference<>() {}); } catch (Exception e) { return List.of(); } }
    private List<CodingProblemDraft.TestCase> readTests(String json) { try { return objectMapper.readValue(json, new TypeReference<>() {}); } catch (Exception e) { return List.of(); } }
    private User user(Long id) { return userRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다.")); }
}
