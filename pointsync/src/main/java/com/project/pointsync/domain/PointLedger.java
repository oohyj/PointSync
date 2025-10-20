package com.project.pointsync.domain;

import com.project.pointsync.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "point_ledger")
public class PointLedger extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private int amount;

    @Column(nullable = false, length = 50)
    private String reason;

    private PointLedger(User user, int amount, String reason) {
        this.user = user;
        this.amount = amount;
        this.reason = reason;
    }

    public static PointLedger create(User user, int amount, String reason) {
        return new PointLedger(user, amount, reason);
    }
}

