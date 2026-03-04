package fintech.infra.persistence;


import fintech.common.domain.entity.Settlement;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SettlementJpaRepository extends JpaRepository<Settlement, Long> {

    // 특정 가맹정의 특정 날짜 정산 데이터가 이미 존재하는지 확인 (중복 처리 방지)
    Optional<Settlement> findByMerchantIdAndSettlementDate(String merchantId, LocalDateTime settlementDate);
}
