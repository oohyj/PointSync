package com.project.pointsync.dto;

public record SummaryResult(
        boolean attendedToday,
        int totalPoints,
        int currentStreak,
        int longestStreak
) {}