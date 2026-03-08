package fintech.domain.repository;

import fintech.common.domain.entity.Settlement;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SettlementRepository extends JpaRepository<Settlement, Long> {
    // 멱등성 체크를 위한 필수 메서드
    boolean existsByOrderId(String orderId);

    // 일일 합계 대사를 위한 메서드 (운영 절차용)
    @Query("SELECT SUM(s.amount) FROM Settlement s WHERE s.createdAt >= :start AND s.createdAt < :end")
    BigDecimal sumTotalByDate(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}