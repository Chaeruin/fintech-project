package fintech.infra.kafka;


import fintech.application.SettlementService;
import fintech.common.domain.dto.event.PaymentCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventConsumer {

    private final SettlementService settlementService;

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
            log.info("정산 서비스 - 결제 이벤트 수신: OrderId = {}", event.orderId());
            settlementService.processPaymentEvent(event);
        } catch (Exception e) {
            log.error("이벤트 처리 중 치명적 오류 발생 (사후 조치 필요): {}", event.orderId(), e);
        }
    }
}
