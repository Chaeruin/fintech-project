package fintech.application;


import fintech.application.dto.PaymentConfirmCommand;
import fintech.common.PgClient;
import fintech.common.domain.dto.event.PaymentCompletedEvent;
import fintech.common.domain.entity.Payment;
import fintech.common.domain.enums.PaymentStatus;
import fintech.common.global.exception.CustomException;
import fintech.common.global.exception.ErrorCode;
import fintech.domain.repository.PaymentRepository;
import fintech.domain.service.PaymentProcessor;
import fintech.domain.service.PaymentValidator;
import fintech.infra.kafka.PaymentEventProducer;
import fintech.infra.persistence.PaymentJpaRepository;
import fintech.infra.pg.PaymentProcessorFactory;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentProcessorFactory paymentProcessorFactory; // PG사 선택기
    private final PaymentJpaRepository paymentRepository;         // DB 저장소
    private final PaymentValidator paymentValidator;
    private final PaymentEventPublisher eventPublisher;

    @Transactional
    public void completePayment(PaymentConfirmCommand command) {
        log.info("[PaymentService] 결제 프로세스 시작: OrderId={}, PG={}", command.orderId(), command.pgType());

        // 기존 결제 대기 데이터 조회
        Payment payment = paymentRepository.findByOrderId(command.orderId())
                .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_NOT_FOUND));

        // 비즈니스 규칙 검증
        paymentValidator.validateAction(payment, command.amount());

        // PG사에 맞는 Processor 선택
        PaymentProcessor processor = paymentProcessorFactory.getProcessor(command.pgType());

        // 실제 PG사 승인 요청 (내부적으로 PgClient의 confirm 호출)
        // 성공 시 PG 승인번호(pgConfirmId)를 반환
        String pgConfirmId = processor.pay(command.amount(), command.orderId());

        //  결제 상태 업데이트
        payment.complete(pgConfirmId);

        // 5. Kafka 이벤트 발행
        PaymentCompletedEvent event = new PaymentCompletedEvent(
                pgConfirmId,
                payment.getOrderId(),
                payment.getAmount(),
                payment.getType(),
                payment.getMerchantId(),
                payment.getCreatedAt()
        );
        eventPublisher.publish(event);

        log.info("[PaymentService] 결제 완료 및 이벤트 발행: OrderId={}", command.orderId());
    }
}
