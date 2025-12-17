package com.project.pointsync.dto.PointLedger;

import com.project.pointsync.domain.PointLedger;

public record PointLedgerReqDto (
    Long userId,
    int amount,      // 양수: 적립, 음수: 차감
    PointLedger.PointReason reason
) {}
