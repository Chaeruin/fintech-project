package fintech.infra.kafka;

import fintech.application.SettlementService;
import fintech.dto.PaymentEvent;
import fintech.infra.persistence.entity.FailedEvent;
import fintech.infra.persistence.repository.FailedEventJpaRepository;
import java.time.LocalDateTime;
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
    private final SettlementService settlementService;

    @Scheduled(fixedDelay = 60000) // 1분마다 실행
    @Transactional
    public void retryFailedEvents() {
        List<FailedEvent> targetEvents = failedEventRepository.findTop10ByStatusOrderByCreatedAtAsc("FAIL");

        if (targetEvents.isEmpty()) return;

        log.info("[재처리] 실패 이벤트 {}건 재전송 시도", targetEvents.size());

        for (FailedEvent failedEvent : targetEvents) {
            try {
                PaymentEvent paymentEvent = settlementService.parseEvent(failedEvent.getPayload());
                settlementService.processSettlement(paymentEvent);

                failedEventRepository.delete(failedEvent);

                log.info("재처리 성공: {}", failedEvent.getEventKey());
            } catch (Exception e) {
                failedEvent = FailedEvent.builder()
                        .id(failedEvent.getId())
                        .topic(failedEvent.getTopic())
                        .eventKey(failedEvent.getEventKey())
                        .payload(failedEvent.getPayload())
                        .errorMessage(e.getMessage())
                        .retryCount(failedEvent.getRetryCount() + 1)
                        .createdAt(LocalDateTime.now())
                        .build();

                failedEventRepository.save(failedEvent);

                log.warn("재처리 실패: {} (retry={})",
                        failedEvent.getEventKey(), failedEvent.getRetryCount());
            }
        }
    }
}
