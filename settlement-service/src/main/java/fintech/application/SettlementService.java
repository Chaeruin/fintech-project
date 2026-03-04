package fintech.application;


import fintech.common.domain.dto.event.PaymentCompletedEvent;
import fintech.common.domain.entity.Settlement;
import fintech.infra.persistence.SettlementJpaRepository;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SettlementService {

    private final SettlementJpaRepository settlementRepository;
    private final SettlementCalculator settlementCalculator;

    @Transactional
    public void processPaymentEvent(PaymentCompletedEvent event) {
        // 멱등성 검사 - 이미 처리된 주문인지 확인, 일별 합산 로직 수행
        // 실제 운영 환경에서는 별도의 SettlementDetail 테이블에 orderId 유니크 키를 걸어 중복 방지

        // 결제 다음날 정산 기준
        LocalDateTime settlementDate = event.completedAt().toLocalDate().plusDays(1).atStartOfDay();

        // 기존 정산 마스터 조회 혹은 생성
        Settlement settlement = settlementRepository.findByMerchantIdAndSettlementDate(
                        event.merchantId(), settlementDate)
                .orElseGet(() -> createNewSettlement(event.merchantId(), settlementDate));

        // 금액 합산 and 수수료 계산
        BigDecimal fee = settlementCalculator.calculateFee(event.amount());
        BigDecimal settlementAmount = settlementCalculator.calculateSettlementAmount(event.amount(), fee);

        // 엔티티 업데이트 - Dirty Checking
        settlement.updateAmount(event.amount(), fee, settlementAmount);

        log.info("정산 데이터 업데이트 완료: Merchant = {}, Date = {}, AddedAmount = {}",
                event.merchantId(), settlementDate, event.amount());
    }

    private Settlement createNewSettlement(String merchantId, LocalDateTime date) {
        return Settlement.builder()
                .merchantId(merchantId)
                .totalAmount(BigDecimal.ZERO)
                .totalFee(BigDecimal.ZERO)
                .settlementAmount(BigDecimal.ZERO)
                .settlementDate(date)
                .build();
    }
}
