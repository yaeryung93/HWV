package com.example.backend.controller;

import com.example.backend.dto.GenerateQuizRequest;
import com.example.backend.entity.Quiz;
import com.example.backend.service.QuizService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/quiz")
public class QuizController {

    private final QuizService quizService;

    public QuizController(QuizService quizService) {
        this.quizService = quizService;
    }

    @PostMapping("/generate")
    public List<Quiz> generate(@RequestBody GenerateQuizRequest request) {

        return quizService.generateQuiz(
                request.getSummary(),
                request.getUserId()
        );
    }
}
