package com.project.pointsync.service;

import com.project.pointsync.domain.AttendanceLog;
import com.project.pointsync.domain.User;
import com.project.pointsync.dto.CheckInResult;
import com.project.pointsync.dto.SummaryResult;
import com.project.pointsync.global.time.TimeProvider;
import com.project.pointsync.repository.AttendanceLogRepository;
import com.project.pointsync.repository.PointLedgerRepository;
import com.project.pointsync.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AttendanceLogService {

    private final AttendanceLogRepository attendanceLogRepository;
    private final PointLedgerRepository pointLedgerRepository;
    private final UserRepository userRepository;
    private final StringRedisTemplate redis;
    private final TimeProvider time;

    /**
     * KST 기준 오늘 출석 체크(멱등)
     * - 캐시 키가 있으면 DB 접근 없이 바로 성공
     * - 없으면 저장 시도(유니크 제약 흡수) 후 자정까지 TTL로 캐시 기록
     */
    @Transactional
    public CheckInResult checkIn(Long userId) {
        LocalDate today = time.today();
        String cacheKey = "attendance:" + userId + ":" + today;

        // 1) 캐시 히트 → 멱등 성공
        if (Boolean.TRUE.equals(redis.hasKey(cacheKey))) {
            int totalPoints = pointLedgerRepository.sumAmountByUserId(userId);
            int currentStreak = calculateCurrentStreak(userId, today);
            int longestStreak = calculateLongestStreak(userId);
            return new CheckInResult(true, today, 0, totalPoints, currentStreak, longestStreak);
        }

        // 2) DB 저장 시도 (유니크 제약으로 하루 1회 보장)
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + userId));
        try {
            attendanceLogRepository.save(AttendanceLog.create(user, today));
        } catch (DataIntegrityViolationException ignore) {
            // 동일 (userId, date) 이미 존재 → 멱등 처리
        }

        // 3) 캐시 기록(자정까지 TTL)
        long ttlSec = time.secondsUntilMidnight();
        redis.opsForValue().set(cacheKey, "1", ttlSec, TimeUnit.SECONDS);

        int totalPoints = pointLedgerRepository.sumAmountByUserId(userId);
        int currentStreak = calculateCurrentStreak(userId, today);
        int longestStreak = calculateLongestStreak(userId);
        return new CheckInResult(true, today, 0, totalPoints, currentStreak, longestStreak);
    }

    /** 기간 내 출석일 목록(캘린더 표시용) */
    public List<LocalDate> getCalendar(Long userId, LocalDate from, LocalDate to) {
        return attendanceLogRepository.findDatesByUserIdAndRange(userId, from, to);
    }

    /** 오늘 출석 여부/누적 포인트/연속일수 요약 */
    public SummaryResult getSummary(Long userId) {
        LocalDate today = time.today();
        boolean attendedToday = attendanceLogRepository.existsByUserIdAndAttendDate(userId, today);
        int totalPoints = pointLedgerRepository.sumAmountByUserId(userId);
        int currentStreak = calculateCurrentStreak(userId, today);
        int longestStreak = calculateLongestStreak(userId);
        return new SummaryResult(attendedToday, totalPoints, currentStreak, longestStreak);
    }

    /** 오늘 포함 연속 출석 */
    private int calculateCurrentStreak(Long userId, LocalDate today) {
        int streak = 0;
        LocalDate d = today;
        while (attendanceLogRepository.existsByUserIdAndAttendDate(userId, d)) {
            streak++;
            d = d.minusDays(1);
        }
        return streak;
    }

    /** 최장 연속 출석(간단: 최근 365일 조회 후 스캔) */
    private int calculateLongestStreak(Long userId) {
        LocalDate to = time.today();
        LocalDate from = to.minusDays(365);
        List<LocalDate> days = attendanceLogRepository.findDatesByUserIdAndRange(userId, from, to);
        if (days.isEmpty()) return 0;

        int longest = 1, curr = 1;
        for (int i = 1; i < days.size(); i++) {
            if (days.get(i).minusDays(1).equals(days.get(i - 1))) {
                curr++;
            } else {
                longest = Math.max(longest, curr);
                curr = 1;
            }
        }
        return Math.max(longest, curr);
    }


}
