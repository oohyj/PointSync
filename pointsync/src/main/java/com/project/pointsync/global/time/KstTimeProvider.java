package com.project.pointsync.global.time;

import org.springframework.stereotype.Component;

import java.time.*;
import java.time.temporal.ChronoUnit;

@Component
public class KstTimeProvider implements TimeProvider {

    private final Clock clock;

    public KstTimeProvider(Clock clock) {
        this.clock = clock;
    }

    @Override
    public ZoneId zone() {
        return clock.getZone();
    }

    @Override
    public LocalDate today() {
        return LocalDate.now(clock);
    }

    @Override
    public long secondsUntilMidnight() {
        ZonedDateTime now = ZonedDateTime.now(clock);
        ZonedDateTime nextMidnight = now.truncatedTo(ChronoUnit.DAYS).plusDays(1);
        return Duration.between(now, nextMidnight).getSeconds();
    }
}