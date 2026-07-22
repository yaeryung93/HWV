package com.example.backend.service;

import com.example.backend.dto.QuizResultRequest;
import com.example.backend.entity.*;
import com.example.backend.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class QuizService {
    private final QuizRepository quizRepository;
    private final QuizAttemptRepository attemptRepository;
    private final UserRepository userRepository;
    private final CodingProblemRepository codingProblemRepository;
    private final CodingSubmissionRepository codingSubmissionRepository;

    public QuizService(QuizRepository quizRepository,
                       QuizAttemptRepository attemptRepository, UserRepository userRepository,
                       CodingProblemRepository codingProblemRepository, CodingSubmissionRepository codingSubmissionRepository) {
        this.quizRepository = quizRepository;
        this.attemptRepository = attemptRepository; this.userRepository = userRepository;
        this.codingProblemRepository = codingProblemRepository; this.codingSubmissionRepository = codingSubmissionRepository;
    }

    @Transactional
    public List<Quiz> generateQuiz(String code, Long userId) {
        return getLatestQuiz(userId);
    }

    public List<Quiz> getLatestQuiz(Long userId) {
        List<Quiz> quizzes = new ArrayList<>(quizRepository.findTop3ByUserOrderByCreatedAtDescIdDesc(user(userId)));
        Collections.reverse(quizzes);
        return quizzes;
    }

    @Transactional
    public Map<String, Object> saveResult(QuizResultRequest request) {
        User user = user(request.getUserId());
        int correct = 0;
        List<Map<String, Object>> wrongAnswers = new ArrayList<>();
        for (QuizResultRequest.Answer answer : request.getAnswers()) {
            Quiz quiz = quizRepository.findById(answer.getQuizId()).orElseThrow(() -> new IllegalArgumentException("문제를 찾을 수 없습니다."));
            if (!quiz.getUser().getId().equals(user.getId())) throw new IllegalArgumentException("다른 사용자의 문제입니다.");
            boolean isCorrect = answer.getSelectedAnswer() != null && answer.getSelectedAnswer() == quiz.getAnswer();
            QuizAttempt attempt = new QuizAttempt(); attempt.setUser(user); attempt.setQuiz(quiz);
            attempt.setSelectedAnswer(answer.getSelectedAnswer() == null ? 0 : answer.getSelectedAnswer()); attempt.setCorrect(isCorrect);
            attemptRepository.save(attempt);
            if (isCorrect) correct++; else wrongAnswers.add(Map.of("quizId", quiz.getId(), "question", quiz.getQuestion(),
                "grammarName", quiz.getGrammarName(), "selectedAnswer", attempt.getSelectedAnswer(), "correctAnswer", quiz.getAnswer(),
                "explanation", quiz.getExplanation()));
        }
        return Map.of("correctCount", correct, "wrongCount", request.getAnswers().size() - correct, "wrongAnswers", wrongAnswers);
    }

    public Map<String, Object> dashboard(Long userId) {
        User user = user(userId); long generated = quizRepository.countByUser(user) + codingProblemRepository.countByUser(user);
        long correct = attemptRepository.countByUserAndCorrect(user, true) + codingSubmissionRepository.countByUserAndPassed(user, true);
        long wrong = attemptRepository.countByUserAndCorrect(user, false) + codingSubmissionRepository.countByUserAndPassed(user, false);
        long total = correct + wrong;
        List<Map<String, Object>> codingRecent = codingSubmissionRepository.findByUserOrderBySubmittedAtDesc(user).stream().limit(4).map(this::codingAttemptMap).toList();
        List<Map<String, Object>> recent = codingRecent.isEmpty()
            ? attemptRepository.findByUserOrderByAnsweredAtDesc(user).stream().limit(4).map(this::attemptMap).toList() : codingRecent;
        Map<String, Object> result = new LinkedHashMap<>(); result.put("generatedProblems", generated); result.put("correctAnswers", correct);
        result.put("incorrectAnswers", wrong); result.put("accuracy", total == 0 ? 0 : Math.round(correct * 100.0 / total)); result.put("recentAttempts", recent);
        return result;
    }

    public List<Map<String, Object>> wrongNotes(Long userId) {
        User user = user(userId);
        List<Map<String, Object>> coding = codingSubmissionRepository.findByUserOrderBySubmittedAtDesc(user).stream()
            .filter(item -> !item.isPassed()).map(this::codingAttemptMap).toList();
        return coding.isEmpty() ? attemptRepository.findByUserOrderByAnsweredAtDesc(user).stream().filter(a -> !a.isCorrect()).map(this::attemptMap).toList() : coding;
    }

    public Map<String, Object> statistics(Long userId) {
        User user = user(userId); Map<String, Object> result = new LinkedHashMap<>(dashboard(userId));
        List<QuizAttempt> attempts = attemptRepository.findByUserOrderByAnsweredAtDesc(user);
        List<CodingSubmission> codingAttempts = codingSubmissionRepository.findByUserOrderBySubmittedAtDesc(user);
        Map<String, List<CodingSubmission>> codingByGrammar = codingAttempts.stream().collect(Collectors.groupingBy(a -> a.getProblem().getGrammarName()));
        if (!codingByGrammar.isEmpty()) result.put("categoryAccuracy", codingByGrammar.entrySet().stream().map(e -> Map.of("name", e.getKey(), "value",
            Math.round(e.getValue().stream().filter(CodingSubmission::isPassed).count() * 100.0 / e.getValue().size()))).toList());
        else {
            Map<String, List<QuizAttempt>> byGrammar = attempts.stream().collect(Collectors.groupingBy(a -> a.getQuiz().getGrammarName()));
            result.put("categoryAccuracy", byGrammar.entrySet().stream().map(e -> Map.of("name", e.getKey(), "value",
                Math.round(e.getValue().stream().filter(QuizAttempt::isCorrect).count() * 100.0 / e.getValue().size()))).toList());
        }
        List<Long> weekly = new ArrayList<>(); List<Map<String, Object>> dailyAccuracy = new ArrayList<>(); LocalDate today = LocalDate.now();
        for (int i = 6; i >= 0; i--) { LocalDate day = today.minusDays(i); LocalDateTime start = day.atStartOfDay(), end = day.plusDays(1).atStartOfDay();
            long dailyTotal, dailyCorrect;
            if (codingAttempts.isEmpty()) {
                List<QuizAttempt> dailyAttempts = attempts.stream()
                    .filter(a -> !a.getAnsweredAt().isBefore(start) && a.getAnsweredAt().isBefore(end)).toList();
                dailyTotal = dailyAttempts.size();
                dailyCorrect = dailyAttempts.stream().filter(QuizAttempt::isCorrect).count();
            } else {
                List<CodingSubmission> dailyAttempts = codingAttempts.stream()
                    .filter(a -> !a.getSubmittedAt().isBefore(start) && a.getSubmittedAt().isBefore(end)).toList();
                dailyTotal = dailyAttempts.size();
                dailyCorrect = dailyAttempts.stream().filter(CodingSubmission::isPassed).count();
            }
            weekly.add(dailyTotal);
            dailyAccuracy.add(Map.of(
                "date", day.toString(),
                "correct", dailyCorrect,
                "total", dailyTotal,
                "accuracy", dailyTotal == 0 ? 0 : Math.round(dailyCorrect * 100.0 / dailyTotal)
            ));
        }
        result.put("weeklyAttempts", weekly); result.put("dailyAccuracy", dailyAccuracy); return result;
    }

    private Map<String, Object> attemptMap(QuizAttempt a) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", a.getId()); result.put("quizId", a.getQuiz().getId()); result.put("problemId", a.getQuiz().getId());
        result.put("problemTitle", a.getQuiz().getQuestion()); result.put("grammarName", a.getQuiz().getGrammarName());
        result.put("correct", a.isCorrect()); result.put("selectedAnswer", a.getSelectedAnswer());
        result.put("correctAnswer", a.getQuiz().getAnswer()); result.put("explanation", a.getQuiz().getExplanation());
        result.put("submittedAt", a.getAnsweredAt().toString()); result.put("passedCount", a.isCorrect() ? 1 : 0);
        result.put("totalCount", 1); result.put("passed", a.isCorrect());
        return result;
    }
    private Map<String, Object> codingAttemptMap(CodingSubmission item) {
        Map<String, Object> result = new LinkedHashMap<>(); result.put("id", item.getId()); result.put("problemId", item.getProblem().getId());
        result.put("problemTitle", item.getProblem().getTitle()); result.put("grammarName", item.getProblem().getGrammarName());
        result.put("passedCount", item.getPassedCount()); result.put("totalCount", item.getTotalCount()); result.put("passed", item.isPassed());
        result.put("hint", item.getHint()); result.put("explanation", item.getImprovement()); result.put("submittedAt", item.getSubmittedAt().toString());
        return result;
    }
    private User user(Long id) { return userRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다.")); }
}
