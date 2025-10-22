package com.project.pointsync.controller;

import com.project.pointsync.dto.PointLedger.PointLedgerListResDto;
import com.project.pointsync.dto.PointLedger.PointLedgerReqDto;
import com.project.pointsync.dto.PointLedger.PointLedgerResDto;
import com.project.pointsync.dto.PointLedger.PointTotalResDto;
import com.project.pointsync.service.PointLedgerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/points")
@RequiredArgsConstructor
public class PointLedgerController {

    private final PointLedgerService pointLedgerService;

    /** 포인트 원장 기록 추가 :적립&차감 */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PointLedgerResDto create(@RequestBody PointLedgerReqDto req) {
        return pointLedgerService.create(req);
    }

    /** 사용자별 포인트 총합 조회  */
    @GetMapping("/total")
    public PointTotalResDto getTotal(@RequestParam Long userId) {
        return pointLedgerService.getTotal(userId);
    }

    /** 사용자별 포인트 이력 페이지 조회 : 최신순 */
    @GetMapping("/history")
    public PointLedgerListResDto getHistory(@RequestParam Long userId,
                                            @RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "10") int size) {
        return pointLedgerService.getHistory(userId, page, size);
    }
}