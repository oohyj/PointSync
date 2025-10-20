package com.project.pointsync.domain;

import com.project.pointsync.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table( // 사용자는 하루에 한번만 출석할 수 있음
        name = "attendance_log",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_user_day", columnNames = {"user_id", "attend_date"})} )
public class AttendanceLog extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // KST 기준
    @Column(name = "attend_date", nullable = false)
    private LocalDate attendDate;

    private AttendanceLog(User user, LocalDate attendDate) {
        this.user = user;
        this.attendDate = attendDate;
    }

    public static AttendanceLog create(User user, LocalDate attendDate) {
        return new AttendanceLog(user, attendDate);
    }
}