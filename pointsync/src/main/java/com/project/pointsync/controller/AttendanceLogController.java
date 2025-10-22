package com.project.pointsync.controller;

import com.project.pointsync.dto.AttendanceLog.CheckInResult;
import com.project.pointsync.dto.AttendanceLog.SummaryResult;
import com.project.pointsync.service.AttendanceLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/attendances")
@RequiredArgsConstructor
public class AttendanceLogController {

    private final AttendanceLogService attendanceLogService;

    /** 출석체크 */
    @PostMapping("/check-in")
    @ResponseStatus(HttpStatus.CREATED)
    public CheckInResult checkIn(@RequestParam Long userId) {
        return attendanceLogService.checkIn(userId);
    }

    /** 기간별 캘린더 조회  */
    @GetMapping("/me")
    public List<LocalDate> getCalendar(
            @RequestParam Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return attendanceLogService.getCalendar(userId, from, to);
    }

    /** 출석 요약 정보 */
    @GetMapping("/summary")
    public SummaryResult getSummary(@RequestParam Long userId) {
        return attendanceLogService.getSummary(userId);
    }
}
