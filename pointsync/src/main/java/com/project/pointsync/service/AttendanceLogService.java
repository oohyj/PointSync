package com.project.pointsync.service;

import com.project.pointsync.domain.AttendanceLog;
import com.project.pointsync.domain.PointLedger;
import com.project.pointsync.domain.User;
import com.project.pointsync.dto.AttendanceLog.CheckInResult;
import com.project.pointsync.dto.AttendanceLog.SummaryResult;
import com.project.pointsync.global.exception.CustomException;
import com.project.pointsync.global.exception.ErrorCode;
import com.project.pointsync.global.time.TimeProvider;
import com.project.pointsync.repository.AttendanceLogRepository;
import com.project.pointsync.repository.PointLedgerRepository;
import com.project.pointsync.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
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

      long ttlSec = time.secondsUntilMidnight();
      boolean first = Boolean.TRUE.equals(
              redis.opsForValue().setIfAbsent(cacheKey,"1" ,java.time.Duration.ofSeconds(ttlSec))
      );

      User user = userRepository.findById(userId)
              .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

      int todayPoint = 0;

      if(first){
          try{
              attendanceLogRepository.save(AttendanceLog.create(user , today));

              pointLedgerRepository.save(PointLedger.create(user , 1 , PointLedger.PointReason.DAILY_CHECK_IN ));
              todayPoint = 1;
          }catch (DataIntegrityViolationException ignore){
              log.info("이미 같은 날짜의 출석 로그가 존재 (race condition) userId={}, date={}", userId, today);
          }

      }
        int totalPoints = pointLedgerRepository.sumAmountByUserId(userId);
        int currentStreak = calculateCurrentStreak(userId, today);
        int longestStreak = calculateLongestStreak(userId);

        return new CheckInResult(true, today, todayPoint, totalPoints, currentStreak, longestStreak);

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
