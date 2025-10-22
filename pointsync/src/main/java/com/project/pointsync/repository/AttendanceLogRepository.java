package com.project.pointsync.repository;

import com.project.pointsync.domain.AttendanceLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface AttendanceLogRepository extends JpaRepository<AttendanceLog, Long> {

    boolean existsByUserIdAndAttendDate(Long userId, LocalDate attendDate);

    @Query("""
           select a.attendDate
           from AttendanceLog a
           where a.user.id = :userId
             and a.attendDate between :from and :to
           order by a.attendDate
           """)
    List<LocalDate> findDatesByUserIdAndRange(Long userId, LocalDate from, LocalDate to);

}