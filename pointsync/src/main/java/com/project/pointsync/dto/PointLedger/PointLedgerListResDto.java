package com.project.pointsync.dto.PointLedger;

import java.util.List;

public record PointLedgerListResDto(
        Long userId,
        int page,
        int size,
        long totalElements,
        int totalPages,
        List<PointLedgerResDto> items
) {
}
