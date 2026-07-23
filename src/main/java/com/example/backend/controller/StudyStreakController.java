package com.example.backend.controller;

import com.example.backend.dto.StudyStreakResponse;
import com.example.backend.service.StudyStreakService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StudyStreakController {
    private final StudyStreakService studyStreakService;

    public StudyStreakController(StudyStreakService studyStreakService) {
        this.studyStreakService = studyStreakService;
    }

    @GetMapping("/api/me/streak")
    public StudyStreakResponse streak(@RequestParam Long userId) {
        return studyStreakService.getStreak(userId);
    }
}
