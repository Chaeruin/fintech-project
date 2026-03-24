package fintech.infra.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import fintech.domain.entity.OutboxEvent;
import fintech.domain.repository.OutboxRepository;
import fintech.event.PaymentCompletedEvent;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxPublisher {

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    //INIT 상태인 이벤트를 찾아 Kafka로 발행
    @Scheduled(fixedDelay = 1000)
    @Transactional
    public void publish() {
        // 아직 발행되지 않은(INIT) 이벤트 조회
        List<OutboxEvent> pendingEvents =
                outboxRepository.findAllByAggregateTypeAndEventType("PAYMENT", "INIT");

        for (OutboxEvent event : pendingEvents) {
            try {
                // JSON → 객체 변환
                PaymentCompletedEvent payload =
                        objectMapper.readValue(event.getPayload(), PaymentCompletedEvent.class);

                kafkaTemplate.send(
                        event.ge,
                        payload
                ).whenComplete((result, ex) -> {

                    if (ex == null) {
                        log.info("Outbox → Kafka 전송 성공: {}", event.getEventKey());
                        updateToPublished(event.getId());
                    } else {
                        log.error("Outbox 전송 실패: {}", event.getEventKey(), ex);
                        // ❗ 상태 유지 (INIT) → 다음 스케줄에서 재시도
                    }
                });

            } catch (Exception e) {
                log.error("Outbox payload 역직렬화 실패: {}", event.getId(), e);
            }
        }
    }

    // 별도 트랜잭션으로 상태를 "PROCESSED"로 업데이트하여 발행 완료 처리
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateToPublished(Long outboxId) {
        outboxRepository.findById(outboxId).ifPresent(event -> {
            event.markPublished();
            outboxRepository.save(event);
            log.info(">>> Outbox 이벤트 발행 완료 처리 (ID: {})", outboxId);
        });
    }
}
