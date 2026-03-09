package fintech.application;


import fintech.application.dto.PaymentConfirmCommand;
import fintech.common.domain.dto.event.PaymentCompletedEvent;
import fintech.common.domain.entity.Payment;
import fintech.common.domain.enums.PaymentType;
import fintech.common.global.exception.CustomException;
import fintech.common.global.exception.ErrorCode;
import fintech.domain.repository.PaymentJpaRepositoryImpl;
import fintech.domain.service.PaymentProcessor;
import fintech.domain.service.PaymentValidator;
import fintech.infra.kafka.PaymentEventProducer;
import fintech.infra.persistence.FailedEventJpaRepository;
import fintech.infra.persistence.PaymentJpaRepository;
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
    private final PaymentJpaRepository paymentRepository;         // DB 저장소
    private final PaymentValidator paymentValidator;
    private final PaymentEventProducer paymentEventProducer;
    private final FailedEventJpaRepository failedEventRepository;

    @Transactional
    public void completePayment(PaymentConfirmCommand command) {
        log.info("[PaymentService] 결제 프로세스 시작: OrderId={}, PG={}", command.orderId(), command.pgType());

        // 기존 결제 대기 데이터 조회 - 사전 검증 (계정계 원장 확인)
        Payment payment = paymentRepository.findByOrderId(command.orderId())
                .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_NOT_FOUND));

        // 비즈니스 규칙 검증
        paymentValidator.validateAction(payment, command.amount());

        // PG사에 맞는 Processor 선택 (채널계 어댑터)
        PaymentProcessor processor = paymentProcessorFactory.getProcessor(command.pgType());

        // 실제 PG사 승인 요청 (내부적으로 PgClient의 confirm 호출)
        // 성공 시 PG 승인번호(pgConfirmId)를 반환
        String pgConfirmId;
        try {
            pgConfirmId = processor.pay(command.amount(), command.orderId());
        } catch (Exception e) {
            log.error("PG 승인 통신 실패: OrderId={}", command.orderId());
            throw new CustomException(ErrorCode.PG_CONFIRM_FAILED);
        }

        // 비즈니스 로직 및 이벤트 발행 (정보계 전달)
        try {
            // 원장(결제 상태) 업데이트
            payment.complete(pgConfirmId);
            paymentRepository.save(payment);

            // Kafka 이벤트 발행 시도
            PaymentCompletedEvent event = new PaymentCompletedEvent(
                    pgConfirmId,
                    payment.getOrderId(),
                    payment.getAmount(),
                    payment.getType(),
                    payment.getMerchantId(),
                    payment.getCreatedAt()
            );
            paymentEventProducer.sendPaymentCompletedEvent(event);

            log.info("[PaymentService] 결제 완료 및 이벤트 발행: OrderId={}", command.orderId());

        } catch (Exception e) {
            // 보상 트랜잭션 시작
            // DB 저장 실패나 런타임 예외 시 PG 결제 취소
            handleCompensation(processor, pgConfirmId, command.orderId());

            // 재처리 절차 - Outbox 패턴
            // Kafka 발행 실패 시, 재처리 스케줄러가 잡을 수 있도록 DB에 실패 이벤트 기록
            saveFailedEvent(command.orderId(), pgConfirmId, payment.getMerchantId(), payment.getType(), command.amount());

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
