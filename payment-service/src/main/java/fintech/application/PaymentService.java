package fintech.application;


import com.fasterxml.jackson.databind.ObjectMapper;
import fintech.application.dto.PaymentConfirmCommand;
import fintech.domain.entity.IdempotencyKey;
import fintech.domain.entity.OutboxEvent;
import fintech.domain.repository.IdempotencyRepository;
import fintech.domain.repository.OutboxRepository;
import fintech.event.PaymentCompletedEvent;
import fintech.domain.entity.Payment;
import fintech.domain.enums.PaymentType;
import fintech.global.exception.CustomException;
import fintech.global.exception.ErrorCode;
import fintech.domain.service.PaymentProcessor;
import fintech.domain.service.PaymentValidator;
import fintech.infra.kafka.PaymentEventProducer;
import fintech.infra.persistence.repository.FailedEventJpaRepository;
import fintech.infra.persistence.repository.PaymentJpaRepository;
import fintech.infra.persistence.entity.FailedEvent;
import fintech.infra.pg.PaymentProcessorFactory;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentProcessorFactory paymentProcessorFactory; // PG사 선택기
    private final PaymentValidator paymentValidator;
    private final PaymentEventProducer paymentEventProducer;
    private final FailedEventJpaRepository failedEventRepository;
    private final OutboxRepository outboxRepository;
    private final PaymentJpaRepository paymentRepository;
    private final IdempotencyRepository idempotencyRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void completePayment(String idempotencyKey, PaymentConfirmCommand command) {
        // 멱등성 체크
        if (idempotencyRepository.existsByIdempotencyKey(idempotencyKey)) {
            log.warn("[Idempotency] 중복된 결제 요청 차단: Key={}", idempotencyKey);
            throw new CustomException(ErrorCode.DUPLICATE_PAYMENT);
        }

        log.info("[Payment] 결제 프로세스 시작: OrderId={}, Key={}", command.orderId(), idempotencyKey);

        // 2. 데이터 조회 및 사전 검증
        Payment payment = paymentRepository.findByOrderId(command.orderId())
                .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_NOT_FOUND));
        paymentValidator.validateAction(payment, command.amount());

        // 3. PG사 승인 요청 (외부 통신)
        PaymentProcessor processor = paymentProcessorFactory.getProcessor(command.pgType());
        String pgConfirmId;
        try {
            pgConfirmId = processor.pay(command.amount(), command.orderId());
        } catch (Exception e) {
            log.error("PG 승인 통신 실패: OrderId={}", command.orderId());
            throw new CustomException(ErrorCode.PG_CONFIRM_FAILED);
        }

        // 원장 업데이트 + Outbox 저장 + 멱등성 키 저장을 하나의 트랜잭션으로 묶음
        try {
            // 결제 상태 완료 업데이트
            payment.complete(pgConfirmId);

            // 멱등성 키 저장 -  성공한 요청임을 기록
            idempotencyRepository.save(new IdempotencyKey(idempotencyKey, payment.getId()));

            // Kafka 직접 발행 대신 Outbox에 저장 !!!!
            PaymentCompletedEvent event = new PaymentCompletedEvent(
                    pgConfirmId, payment.getOrderId(), payment.getAmount(),
                    payment.getType(), payment.getMerchantId(), payment.getCreatedAt()
            );
            String payload = objectMapper.writeValueAsString(event);

            // INIT 상태로 저장 - Message Relay가 읽어가도록 함
            outboxRepository.save(new OutboxEvent("PAYMENT", payment.getId(), payload));

            log.info("[Payment] 결제 완료 및 Outbox 기록 성공: OrderId={}", command.orderId());

        } catch (Exception e) {
            // DB 작업 실패 시 PG 결제 취소 - 보상 트랜잭션
            handleCompensation(processor, pgConfirmId, command.orderId());
            log.error("[Payment] 내부 로직 실패로 인한 보상 트랜잭션 실행: OrderId={}", command.orderId());
            throw new CustomException(ErrorCode.PAYMENT_PROCESS_FAILED);
        }
    }

    private void handleCompensation(PaymentProcessor processor, String pgConfirmId, String orderId) {
        log.error("결제 후처리 실패 - 보상 트랜잭션(취소) 실행. OrderId={}, PG_ID={}", orderId, pgConfirmId);
        try {
            processor.cancel(pgConfirmId, "Internal System Error - Rollback");
        } catch (Exception cancelEx) {
            // 취소 실패 시 '불일치 알림' 대상이 됨 (운영 절차 3번)
            log.error("Critical: 보상 트랜잭션 실패! 수동 확인 필요. PG_ID={}", pgConfirmId, cancelEx);
        }
    }

    private void saveFailedEvent(String orderId, String pgConfirmId, String merchantId, PaymentType type, BigDecimal amount) {
        // 재처리 시 사용할 이벤트 객체 생성
        PaymentCompletedEvent event = PaymentCompletedEvent.builder()
                .paymentId(pgConfirmId)
                .orderId(orderId)
                .amount(amount)
                .paymentType(type)
                .merchantId(merchantId)
                .completedAt(LocalDateTime.now())
                .build();

        // 객체를 JSON 문자열(payload)로 변환
        String payload = String.format("{\"orderId\":\"%s\", \"amount\":%s}", orderId, amount);

        // 생성자 규격에 맞춰 저장 (topic, eventKey, payload, errorMessage)
        FailedEvent failedEvent = new FailedEvent(
                "payment-completed-topic",
                orderId,
                payload,
                "INTERNAL_PROCESS_ERROR"
        );

        failedEventRepository.save(failedEvent);
    }
}
