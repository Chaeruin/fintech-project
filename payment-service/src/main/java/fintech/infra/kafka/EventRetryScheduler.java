package fintech.infra.kafka;

import fintech.infra.persistence.entity.FailedEvent;
import fintech.infra.persistence.repository.FailedEventJpaRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventRetryScheduler {

    private final FailedEventJpaRepository failedEventRepository;
    private final PaymentEventProducer eventProducer;

    @Scheduled(fixedDelay = 60000) // 1분마다 실행
    @Transactional
    public void retryFailedEvents() {
        List<FailedEvent> targetEvents = failedEventRepository.findTop10ByStatusOrderByCreatedAtAsc("FAIL");

        if (targetEvents.isEmpty()) return;

        log.info("[재처리] 실패 이벤트 {}건 재전송 시도", targetEvents.size());

        for (FailedEvent failedEvent : targetEvents) {
            try {
                // 다시 Kafka로 쏘기
                eventProducer.retry(failedEvent);
                // 성공 시 삭제
                failedEventRepository.delete(failedEvent);
                log.info("[재처리 성공] Event Key: {}", failedEvent.getEventKey());
            } catch (Exception e) {
                log.error("[재처리 실패] 계속 실패 중: {}", failedEvent.getEventKey());
            }
        }
    }
}
