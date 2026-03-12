package fintech.infra.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import fintech.event.PaymentCompletedEvent;
import fintech.infra.persistence.repository.FailedEventJpaRepository;
import fintech.infra.persistence.entity.FailedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final FailedEventJpaRepository failedEventRepository;
    private final ObjectMapper objectMapper;

    private static final String TOPIC_PAYMENT_COMPLETED = "payment-completed";

    public void sendPaymentCompletedEvent(PaymentCompletedEvent event) {
        log.info("Kafka 전송 시작: {}", event.orderId());

        /*
         * 메시지 키 지정: event.orderId()를 키로 사용하여 동일 파티션 할당 (순서 보장)
         * ProducerRecord를 직접 생성하여 전송의 디테일을 제어할 수 있음
         */
        ProducerRecord<String, Object> record = new ProducerRecord<>(
                TOPIC_PAYMENT_COMPLETED,
                event.orderId(), // Message Key
                event
        );

        // 비동기 전송 및 콜백 처리
        kafkaTemplate.send(record).whenComplete((result, ex) -> {
            if (ex == null) {
                // 전송 성공: 파티션 번호와 오프셋 로그 기록
                log.info("Kafka 전송 성공 [Topic: {}] [Partition: {}] [Offset: {}]",
                        result.getRecordMetadata().topic(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            } else {
                // 전송 실패: 브로커 응답 실패 시 DLQ 성격의 DB Table로 이관
                log.error("Kafka 전송 최종 실패 - OrderId: {}, Reason: {}", event.orderId(), ex.getMessage());
                saveFailedEvent(TOPIC_PAYMENT_COMPLETED, event.orderId(), event, ex.getMessage());
            }
        });
    }

    private void saveFailedEvent(String topic, String key, PaymentCompletedEvent event, String errorMessage) {
        try {
            String jsonPayload = objectMapper.writeValueAsString(event);
            FailedEvent failedEvent = new FailedEvent(topic, key, jsonPayload, errorMessage);
            failedEventRepository.save(failedEvent);
            log.warn("실패 이벤트 DB 저장 완료: {}", key);
        } catch (Exception e) {
            log.error("실패 이벤트 DB 저장 중 치명적 에러 발생!", e);
        }
    }
}