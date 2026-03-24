package fintech.application;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fintech.domain.entity.Payment;
import fintech.dto.PaymentEvent;
import fintech.event.PaymentCompletedEvent;
import fintech.domain.entity.Settlement;
import fintech.global.exception.CustomException;
import fintech.domain.service.SettlementCalculator;
import fintech.infra.persistence.SettlementJpaRepository;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.concurrent.TimeUnit;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import fintech.global.exception.ErrorCode;

@Slf4j
@Service
@RequiredArgsConstructor
public class SettlementService {

    private final SettlementJpaRepository settlementRepository;
    private final SettlementCalculator settlementCalculator;
    private final ObjectMapper objectMapper;
    private final RedissonClient redissonClient;

    @Transactional
    public void processPaymentEvent(PaymentCompletedEvent event) {
        // 멱등성 검사 - 이미 처리된 주문인지 확인, 일별 합산 로직 수행
        // 실제 운영 환경에서는 별도의 SettlementDetail 테이블에 orderId 유니크 키를 걸어 중복 방지

        // 결제 다음날 정산 기준
        LocalDateTime settlementDate = event.completedAt().toLocalDate().plusDays(1).atStartOfDay();

        // 기존 정산 마스터 조회 혹은 생성
        Settlement settlement = settlementRepository.findByMerchantIdAndSettlementDate(
                        event.merchantId(), settlementDate)
                .orElseGet(() -> createNewSettlement(event.merchantId(), event.orderId(), settlementDate));

        // 금액 합산 and 수수료 계산
        BigDecimal fee = settlementCalculator.calculateFee(event.amount());
        BigDecimal settlementAmount = settlementCalculator.calculateSettlementAmount(event.amount(), fee);

        // 엔티티 업데이트 - Dirty Checking
        settlement.updateAmount(event.amount(), fee, settlementAmount);

        log.info("정산 데이터 업데이트 완료: Merchant = {}, Date = {}, AddedAmount = {}",
                event.merchantId(), settlementDate, event.amount());
    }

    // 정산 결과 조회
    @Transactional(readOnly = true)
    public Settlement getSettlement(String merchantId, LocalDateTime dateTime) {
        return settlementRepository.findByMerchantIdAndSettlementDate(merchantId, dateTime)
                .orElseThrow(() -> new CustomException(ErrorCode.SETTLEMENT_NOT_FOUND));
    }

    private Settlement createNewSettlement(String merchantId, String orderId, LocalDateTime date) {
        return Settlement.builder()
                .merchantId(merchantId)
                .orderId(orderId)
                .totalAmount(BigDecimal.ZERO)
                .totalFee(BigDecimal.ZERO)
                .settlementAmount(BigDecimal.ZERO)
                .settlementDate(date)
                .build();
    }

    @Transactional
    public void processSettlement(PaymentEvent event) {
        String lockKey = "lock:settlement:" + event.orderId();
        RLock lock = redissonClient.getLock(lockKey);
        Settlement settlement;
        try {
            if (!lock.tryLock(5, 10, TimeUnit.SECONDS)) {
                log.info("[정산 경합] 이미 처리 중인 주문: {}", event.orderId());
                return;
            }
            try {
                // 락 획득 후 DB 중복 체크
                if (settlementRepository.existsByOrderId(event.orderId())) {
                    log.warn("[정산 중복] 이미 정산 완료된 주문: {}", event.orderId());
                    return;
                }
                // 정산 데이터 생성
                settlement = createSettlement(event);

            } finally {
                if (lock.isHeldByCurrentThread()) lock.unlock();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("정산 처리 중 인터럽트 발생");
        }

        log.info("[정산 완료] 주문번호: {}, 정산액: {}, 수수료: {}",
                event.orderId(), settlement.getSettlementAmount(), settlement.getTotalAmount());
    }

    public Settlement createSettlement(PaymentEvent event) {
        // 수수료 및 정산 금액 계산
        BigDecimal feeRate = new BigDecimal("0.03");
        BigDecimal fee = event.amount().multiply(feeRate).setScale(0, RoundingMode.HALF_UP);
        BigDecimal settlementAmount = event.amount().subtract(fee);

        // 정산 엔티티 생성 및 저장
        Settlement settlement = createNewSettlement(event.orderId(), event.merchantId(), event.createdAt());

        settlementRepository.save(settlement);

        return settlement;
    }


    public PaymentEvent parseEvent(String payload) {
        try {
            return objectMapper.readValue(payload, PaymentEvent.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("이벤트 파싱 실패", e);
        }
    }
}
