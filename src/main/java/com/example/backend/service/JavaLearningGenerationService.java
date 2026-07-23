package com.example.backend.service;

import com.example.backend.dto.CodingProblemDraft;
import com.example.backend.dto.CodingProblemTranslationDraft;
import com.example.backend.dto.GeneratedLearningContent;
import com.example.backend.dto.JavaAnalysisResponse;
import com.example.backend.entity.CodingProblem;
import com.example.backend.entity.CodingProblemTranslation;
import com.example.backend.entity.JavaAnalysisCache;
import com.example.backend.entity.User;
import com.example.backend.repository.CodingProblemRepository;
import com.example.backend.repository.CodingProblemTranslationRepository;
import com.example.backend.repository.JavaAnalysisCacheRepository;
import com.example.backend.repository.UserRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;

@Service
public class JavaLearningGenerationService {
    private static final String GENERATION_RULE_VERSION = "multilingual-problems-v3";
    private final GeminiService geminiService;
    private final UserRepository userRepository;
    private final CodingProblemRepository problemRepository;
    private final CodingProblemTranslationRepository translationRepository;
    private final JavaAnalysisCacheRepository cacheRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public JavaLearningGenerationService(GeminiService geminiService, UserRepository userRepository,
                                         CodingProblemRepository problemRepository,
                                         CodingProblemTranslationRepository translationRepository,
                                         JavaAnalysisCacheRepository cacheRepository) {
        this.geminiService = geminiService;
        this.userRepository = userRepository;
        this.problemRepository = problemRepository;
        this.translationRepository = translationRepository;
        this.cacheRepository = cacheRepository;
    }

    @Transactional
    public JavaAnalysisResponse generateAll(Long userId, String code, String difficulty, String language) {
        if (userId == null) throw new IllegalArgumentException("로그인 후 Java 파일을 분석해 주세요.");
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        String normalizedCode = code.replace("\r\n", "\n").replace('\r', '\n');
        String normalizedLanguage = normalizeLanguage(language);
        String sourceHash = hash(GENERATION_RULE_VERSION + "\n" + normalizedLanguage + "\n" + normalizedCode);
        JavaAnalysisCache cached = cacheRepository.findByUserAndSourceHash(user, sourceHash).orElse(null);
        if (cached != null) {
            return new JavaAnalysisResponse(cached.getSummary(), readGrammars(cached.getGrammarsJson()), cached.getSourceCode());
        }

        GeneratedLearningContent generated = geminiService.generateAll(normalizedCode, difficulty, normalizedLanguage);

        List<CodingProblem> savedProblems = problemRepository.saveAll(generated.codingProblems().stream()
            .map(draft -> toEntity(user, draft)).toList());
        saveTranslations(savedProblems, generated.problemTranslations());
        JavaAnalysisCache cache = new JavaAnalysisCache();
        cache.setUser(user);
        cache.setSourceHash(sourceHash);
        cache.setSummary(generated.analysis().summary());
        cache.setGrammarsJson(write(generated.analysis().grammars()));
        cache.setSourceCode(normalizedCode);
        cache.setProblemIdsJson(write(savedProblems.stream().map(CodingProblem::getId).toList()));
        cacheRepository.save(cache);
        return generated.analysis();
    }

    private String normalizeLanguage(String language) {
        return switch (language == null ? "ko" : language.trim().toLowerCase()) {
            case "en" -> "en";
            case "ja" -> "ja";
            default -> "ko";
        };
    }

    private CodingProblem toEntity(User user, CodingProblemDraft draft) {
        CodingProblem problem = new CodingProblem();
        problem.setUser(user);
        problem.setGrammarName(draft.grammarName());
        problem.setTitle(draft.title());
        problem.setDescription(draft.description());
        problem.setRequirementsJson(write(draft.requirements()));
        problem.setInputExample(draft.inputExample());
        problem.setOutputExample(draft.outputExample());
        problem.setStarterCode(draft.starterCode());
        problem.setMethodName(draft.methodName());
        problem.setReturnType(draft.returnType());
        problem.setParameterTypesJson(write(draft.parameterTypes()));
        problem.setDifficulty(draft.difficulty());
        problem.setTestsJson(write(draft.tests()));
        return problem;
    }

    private void saveTranslations(List<CodingProblem> problems,
                                  java.util.Map<String, List<CodingProblemTranslationDraft>> translations) {
        List<CodingProblemTranslation> entities = new java.util.ArrayList<>();
        for (String language : List.of("ko", "en", "ja")) {
            List<CodingProblemTranslationDraft> languageTranslations = translations.get(language);
            if (languageTranslations == null || languageTranslations.size() != problems.size()) {
                throw new IllegalStateException(language + " 문제 번역을 저장할 수 없습니다.");
            }
            for (int index = 0; index < problems.size(); index++) {
                CodingProblemTranslationDraft draft = languageTranslations.get(index);
                CodingProblemTranslation entity = new CodingProblemTranslation();
                entity.setProblem(problems.get(index)); entity.setLanguage(language);
                entity.setGrammarName(draft.grammarName()); entity.setTitle(draft.title());
                entity.setDescription(draft.description()); entity.setSummary(draft.summary());
                entity.setRequirementsJson(write(draft.requirements()));
                entity.setTestNamesJson(write(draft.testNames()));
                entities.add(entity);
            }
        }
        translationRepository.saveAll(entities);
    }

    private List<JavaAnalysisResponse.Grammar> readGrammars(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception exception) {
            throw new IllegalStateException("저장된 Java 분석 결과를 불러오지 못했습니다.", exception);
        }
    }

    private String hash(String code) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(code.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Java 코드 해시를 계산하지 못했습니다.", exception);
        }
    }

    private String write(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalStateException("생성된 학습 데이터를 저장하지 못했습니다.", exception);
        }
    }
}
