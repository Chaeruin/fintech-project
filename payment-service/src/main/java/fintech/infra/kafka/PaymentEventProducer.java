package fintech.infra.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import fintech.common.domain.dto.event.PaymentCompletedEvent;
import fintech.infra.persistence.FailedEventJpaRepository;
import fintech.infra.persistence.entity.FailedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final FailedEventJpaRepository failedEventRepository; // 주입
    private final ObjectMapper objectMapper;

    // 토픽명 -  정산 서비스와 맞춰야 함
    private static final String TOPIC_PAYMENT_COMPLETED = "payment-completed";

    public void sendPaymentCompletedEvent(PaymentCompletedEvent event) {
        log.info("Kafka 전송 시작: {}", event.orderId());

        // orderId를 Key로 사용하여 동일 주문의 이벤트 순서 보장
        kafkaTemplate.send(TOPIC_PAYMENT_COMPLETED, event.orderId(), event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Kafka 전송 성공 - Offset: {}", result.getRecordMetadata().offset());
                    } else {
                        log.error("Kafka 전송 실패 - OrderId: {}", event.orderId(), ex);
                        // 전송 실패 시 사후 처리
                        saveFailedEvent(TOPIC_PAYMENT_COMPLETED, event.orderId(), event, ex.getMessage());
                    }
                });
    }

    private void saveFailedEvent(String topic, String key, PaymentCompletedEvent event, String errorMessage) {
        // 별도의 'failed_events' 테이블에 저장하는 로직
        try {
            String jsonPayload = objectMapper.writeValueAsString(event);
            FailedEvent failedEvent = new FailedEvent(topic, key, jsonPayload, errorMessage);
            failedEventRepository.save(failedEvent);
        } catch (Exception e) {
            log.error("실패 이벤트 저장 중 추가 에러 발생!", e);
        }
    }
}
