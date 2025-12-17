package com.project.pointsync.dto.PointLedger;

import com.project.pointsync.domain.PointLedger;

import java.time.LocalDateTime;

public record PointLedgerResDto(
        Long id,
        Long userId,
        int amount,
        PointLedger.PointReason pointReason,
        LocalDateTime createdAt
) {
    public static PointLedgerResDto from(PointLedger p) {
        return new PointLedgerResDto(
                p.getId(),
                p.getUser().getId(),
                p.getAmount(),
                p.getReason(),
                p.getCreatedAt()
        );
    }
}