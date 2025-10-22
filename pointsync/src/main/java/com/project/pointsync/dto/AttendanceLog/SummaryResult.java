package com.project.pointsync.dto.AttendanceLog;

public record SummaryResult(
        boolean attendedToday,
        int totalPoints,
        int currentStreak,
        int longestStreak
) {}