package com.project.pointsync.global.time;

import java.time.LocalDate;
import java.time.ZoneId;

public interface TimeProvider {
    ZoneId zone();
    LocalDate today();              // KST 기준 오늘
    long secondsUntilMidnight();    // KST 자정까지 남은 초
}
