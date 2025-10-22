package com.project.pointsync.dto.AttendanceLog;

import java.time.LocalDate;

public record CheckInResult(
        boolean attendedToday,
        LocalDate date,
        int todayPoint,
        int totalPoints,
        int currentStreak,
        int longestStreak
) {}