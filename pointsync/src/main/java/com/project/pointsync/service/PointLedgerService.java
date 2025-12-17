package com.project.pointsync.service;

import com.project.pointsync.domain.PointLedger;
import com.project.pointsync.domain.User;
import com.project.pointsync.dto.PointLedger.PointLedgerListResDto;
import com.project.pointsync.dto.PointLedger.PointLedgerReqDto;
import com.project.pointsync.dto.PointLedger.PointLedgerResDto;
import com.project.pointsync.dto.PointLedger.PointTotalResDto;
import com.project.pointsync.global.exception.CustomException;
import com.project.pointsync.global.exception.ErrorCode;
import com.project.pointsync.repository.PointLedgerRepository;
import com.project.pointsync.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PointLedgerService {

    private final PointLedgerRepository pointLedgerRepository;
    private final UserRepository userRepository;

    /** 포인트 원장 기록 추가 :  양수=적립, 음수=차감 */
    @Transactional
    public PointLedgerResDto create(PointLedgerReqDto req) {
        if (req.amount() == 0) {
            throw new IllegalArgumentException("amount는 0일 수 없습니다.");
        }
        User user = userRepository.findById(req.userId())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        PointLedger saved = pointLedgerRepository.save(
                PointLedger.create(user, req.amount(), req.reason())
        );
        return PointLedgerResDto.from(saved);
    }

    /** 사용자별 포인트 총합 조회 */
    public PointTotalResDto getTotal(Long userId) {
        int total = pointLedgerRepository.sumAmountByUserId(userId);
        return new PointTotalResDto(userId, total);
    }

    /**
     * 사용자별 포인트 이력 페이지 조회 (최신순)
     */
    public PointLedgerListResDto getHistory(Long userId, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size);

        Page<PointLedger> resultPage = pointLedgerRepository.findByUserIdOrderByIdDesc(userId, pageable);

        var items = resultPage.getContent().stream()
                .map(PointLedgerResDto::from)
                .collect(Collectors.toList());

        return new PointLedgerListResDto(
                userId,
                page,
                size,
                resultPage.getTotalElements(),
                resultPage.getTotalPages(),
                items
        );
    }

}
