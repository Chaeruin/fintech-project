package fintech.infra.kafka;


import com.fasterxml.jackson.databind.ObjectMapper;
import fintech.application.SettlementService;
import fintech.domain.readentity.SettlementPayment;
import fintech.domain.repository.SettlementPaymentRepository;
import fintech.dto.PaymentEvent;
import fintech.event.PaymentCompletedEvent;
import fintech.domain.repository.SettlementRepository;
import fintech.infra.persistence.entity.FailedEvent;
import fintech.infra.persistence.repository.FailedEventJpaRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventConsumer {

    private final SettlementPaymentRepository settlementPaymentRepository;
    private final FailedEventJpaRepository failedEventRepository;
    private final ObjectMapper objectMapper;

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
            if (settlementPaymentRepository.existsByOrderId(event.orderId())) {
                log.warn("[멱등성] 이미 처리된 정산 이벤트입니다. 스킵합니다. OrderId = {}", event.orderId());
                return;
            }
            SettlementPayment payment = SettlementPayment.builder()
                    .orderId(event.orderId())
                    .transactionKey(event.transactionKey())
                    .amount(event.amount())
                    .status("PAID")
                    .createdAt(LocalDateTime.now())
                    .build();

            settlementPaymentRepository.save(payment);

        } catch (DataIntegrityViolationException e) {
            log.error("[중복] DB 제약조건 위반: {}", event.orderId(), e);
            throw e; // DLQ로 보냄

        } catch (Exception e) {
            log.error("[치명적 실패] 이벤트 저장 실패: {}", event.orderId(), e);
            saveFailedEvent(event, e);
            throw e; // Kafka retry + DLQ
        }
    }

    private void saveFailedEvent(PaymentCompletedEvent event, Exception e) {

        try {
            String payload = objectMapper.writeValueAsString(event);

            FailedEvent failedEvent = FailedEvent.builder()
                    .topic("payment-completed")
                    .eventKey(event.orderId())
                    .payload(payload)
                    .errorMessage(e.getMessage())
                    .retryCount(0)
                    .createdAt(LocalDateTime.now())
                    .build();

            failedEventRepository.save(failedEvent);

        } catch (Exception ex) {
            log.error("FailedEvent 저장 실패", ex);
        }
    }
}
