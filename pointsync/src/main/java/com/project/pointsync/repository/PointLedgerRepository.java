package com.project.pointsync.repository;

import com.project.pointsync.domain.PointLedger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PointLedgerRepository extends JpaRepository<PointLedger, Long> {

    // 누적 포인트 합계(없으면 null -> 0으로 처리)
    @Query("select coalesce(sum(p.amount), 0) from PointLedger p where p.user.id = :userId")
    int sumAmountByUserId(Long userId);

    Page<PointLedger> findByUserIdOrderByIdDesc(Long userId, Pageable pageable);
}