package com.example.backend.dto;

import java.time.LocalDate;

public record StudyStreakResponse(int currentStreak, int longestStreak,
                                  LocalDate lastStudyDate, boolean studiedToday) {
}
