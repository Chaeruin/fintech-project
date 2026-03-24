package fintech.infra.persistence.repository;


import fintech.domain.entity.Settlement;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SettlementJpaRepository extends JpaRepository<Settlement, Long> {

    // 특정 가맹정의 특정 날짜 정산 데이터가 이미 존재하는지 확인 (중복 처리 방지)
    Optional<Settlement> findByMerchantIdAndSettlementDate(String merchantId, LocalDateTime settlementDate);

    @Query("SELECT SUM(s.settlementAmount + s.fee) FROM Settlement s WHERE s.settlementDate = :date")
    Optional<BigDecimal> sumTotalAmountByDate(LocalDate date);

    @Query("SELECT s.orderId FROM Settlement s WHERE s.settlementDate = :date")
    List<String> findOrderIdsByDate(LocalDate date);

    // 멱등성 체크용
    boolean existsByOrderId(String orderId);
}
