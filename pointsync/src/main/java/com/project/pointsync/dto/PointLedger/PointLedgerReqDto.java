package com.project.pointsync.dto.PointLedger;

public record PointLedgerReqDto (
    Long userId,
    int amount,      // 양수: 적립, 음수: 차감
    String reason
) {}
