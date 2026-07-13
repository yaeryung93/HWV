package com.example.backend.service;

import com.example.backend.entity.Quiz;
import com.example.backend.entity.User;
import com.example.backend.repository.QuizRepository;
import com.example.backend.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class QuizService {

    private final GeminiService geminiService;
    private final QuizRepository quizRepository;
    private final UserRepository userRepository;

    public QuizService(GeminiService geminiService,
                       QuizRepository quizRepository,
                       UserRepository userRepository) {

        this.geminiService = geminiService;
        this.quizRepository = quizRepository;
        this.userRepository = userRepository;
    }

    public List<Quiz> generateQuiz(String summary, Long userId) {

        // 로그인한 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        // 기존 퀴즈 삭제
        quizRepository.deleteByUser(user);

        // Gemini로 문제 생성
        List<Quiz> quizList = geminiService.generateQuiz(summary);

        // 사용자 연결
        for (Quiz quiz : quizList) {
            quiz.setUser(user);
        }

        // 저장
        return quizRepository.saveAll(quizList);
    }

    public List<Quiz> getQuiz(User user) {
        return quizRepository.findByUser(user);
    }

    public long getQuizCount(User user) {
        return quizRepository.countByUser(user);
    }
}