package com.example.backend.dto;

import java.util.List;
import java.util.Map;

public record GeneratedLearningContent(
        JavaAnalysisResponse analysis,
        List<CodingProblemDraft> codingProblems,
        Map<String, List<CodingProblemTranslationDraft>> problemTranslations) {
}
