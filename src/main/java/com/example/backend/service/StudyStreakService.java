package com.example.backend.service;

import com.example.backend.dto.StudyStreakResponse;
import com.example.backend.entity.User;
import com.example.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class StudyStreakService {
    private final UserRepository userRepository;

    public StudyStreakService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public void recordSubmission(Long userId, boolean passed) {
        if (!passed) return;

        User user = userRepository.findByIdForUpdate(userId)
            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        applySubmission(user, true, LocalDate.now());
    }

    @Transactional(readOnly = true)
    public StudyStreakResponse getStreak(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        LocalDate today = LocalDate.now();
        LocalDate lastStudyDate = user.getLastStudyDate();
        int currentStreak = lastStudyDate != null && lastStudyDate.isBefore(today.minusDays(1))
            ? 0 : user.getCurrentStreak();
        return new StudyStreakResponse(
            currentStreak,
            user.getLongestStreak(),
            lastStudyDate,
            today.equals(lastStudyDate)
        );
    }

    static void applySubmission(User user, boolean passed, LocalDate today) {
        if (!passed) return;

        LocalDate lastStudyDate = user.getLastStudyDate();
        if (today.equals(lastStudyDate)) return;

        int nextStreak = lastStudyDate != null && lastStudyDate.equals(today.minusDays(1))
            ? user.getCurrentStreak() + 1 : 1;
        user.setCurrentStreak(nextStreak);
        user.setLongestStreak(Math.max(user.getLongestStreak(), nextStreak));
        user.setLastStudyDate(today);
    }
}
