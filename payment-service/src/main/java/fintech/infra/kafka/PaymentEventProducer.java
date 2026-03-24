package fintech.infra.kafka;

import fintech.event.PaymentCompletedEvent;
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
                log.info("Kafka 전송 성공: {}", event.orderId());
            } else {
                log.error("Kafka 전송 실패: {}", event.orderId(), ex);
            }
        });
    }
}