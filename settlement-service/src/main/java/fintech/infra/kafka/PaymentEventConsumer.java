package fintech.infra.kafka;


import fintech.application.SettlementService;
import fintech.common.domain.dto.event.PaymentCompletedEvent;
import fintech.domain.repository.SettlementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventConsumer {

    private final SettlementService settlementService;
    private final SettlementRepository settlementRepository;

    // 결제 완료 이벤트를 수신하여 정산 기초 데이터를 생성
    @KafkaListener(
            topics = "payment-completed",
            groupId = "settlement-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumePaymentEvent(PaymentCompletedEvent event) {
        log.info("정산 서비스 - 결제 이벤트 수신 성공: OrderId = {}, Amount = {}",
                event.orderId(), event.amount());

        try {
            // 멱등성 체크 -  이미 정산 원장에 존재하는 주문인지 확인
            if (settlementRepository.existsByOrderId(event.orderId())) {
                log.warn("[멱등성] 이미 처리된 정산 이벤트입니다. 스킵합니다. OrderId = {}", event.orderId());
                return;
            }

            settlementService.processPaymentEvent(event);
        } catch (Exception e) {
            log.error("이벤트 처리 중 치명적 오류 발생 (사후 조치 필요): {}", event.orderId(), e);
            // 예외 다시 던짐 -> Kafka 설정에 따라 재시도 + DLQ
            throw e;
        }
    }
}
