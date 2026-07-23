package com.example.backend.service;

import com.example.backend.entity.User;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class StudyStreakServiceTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 7, 23);

    @Test
    void firstCorrectSubmissionStartsStreak() {
        User user = new User();

        StudyStreakService.applySubmission(user, true, TODAY);

        assertEquals(1, user.getCurrentStreak());
        assertEquals(1, user.getLongestStreak());
        assertEquals(TODAY, user.getLastStudyDate());
    }

    @Test
    void multipleCorrectSubmissionsOnSameDayDoNotIncreaseStreak() {
        User user = userWithStreak(3, 5, TODAY);

        StudyStreakService.applySubmission(user, true, TODAY);

        assertEquals(3, user.getCurrentStreak());
        assertEquals(5, user.getLongestStreak());
    }

    @Test
    void correctSubmissionAfterYesterdayContinuesStreak() {
        User user = userWithStreak(3, 5, TODAY.minusDays(1));

        StudyStreakService.applySubmission(user, true, TODAY);

        assertEquals(4, user.getCurrentStreak());
        assertEquals(5, user.getLongestStreak());
        assertEquals(TODAY, user.getLastStudyDate());
    }

    @Test
    void correctSubmissionAfterGapRestartsStreak() {
        User user = userWithStreak(4, 7, TODAY.minusDays(2));

        StudyStreakService.applySubmission(user, true, TODAY);

        assertEquals(1, user.getCurrentStreak());
        assertEquals(7, user.getLongestStreak());
        assertEquals(TODAY, user.getLastStudyDate());
    }

    @Test
    void longerCurrentStreakUpdatesLongestStreak() {
        User user = userWithStreak(5, 5, TODAY.minusDays(1));

        StudyStreakService.applySubmission(user, true, TODAY);

        assertEquals(6, user.getCurrentStreak());
        assertEquals(6, user.getLongestStreak());
    }

    @Test
    void failedSubmissionDoesNotChangeStreak() {
        User user = new User();

        StudyStreakService.applySubmission(user, false, TODAY);

        assertEquals(0, user.getCurrentStreak());
        assertEquals(0, user.getLongestStreak());
        assertNull(user.getLastStudyDate());
    }

    private User userWithStreak(int current, int longest, LocalDate lastStudyDate) {
        User user = new User();
        user.setCurrentStreak(current);
        user.setLongestStreak(longest);
        user.setLastStudyDate(lastStudyDate);
        return user;
    }
}
