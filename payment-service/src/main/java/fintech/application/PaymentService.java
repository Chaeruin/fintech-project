package fintech.application;


import fintech.application.dto.PaymentConfirmCommand;
import fintech.common.PgClient;
import fintech.common.domain.dto.event.PaymentCompletedEvent;
import fintech.common.domain.entity.Payment;
import fintech.common.domain.enums.PaymentStatus;
import fintech.common.global.exception.CustomException;
import fintech.common.global.exception.ErrorCode;
import fintech.domain.repository.PaymentRepository;
import fintech.domain.service.PaymentValidator;
import fintech.infra.kafka.PaymentEventProducer;
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

    private final PaymentRepository paymentRepository;
    private final PaymentValidator paymentValidator;
    private final List<PgClient> pgClients;             // 모든 PG 구현체가 리스트로 주입
    private final PaymentEventProducer eventProducer;

    @Transactional
    public void confirm(PaymentConfirmCommand command) {
        // 기존 결제 대기 데이터 조회
        Payment payment = paymentRepository.findByOrderId(command.orderId())
                .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_NOT_FOUND));

        // 비즈니스 규칙 검증
        paymentValidator.validateAction(payment, command.amount());

        // 요청된 pgType에 맞는 클라이언트 선택
        PgClient pgClient = pgClients.stream()
                .filter(client -> client.getPgType().equalsIgnoreCase(command.pgType()))
                .findFirst()
                .orElseThrow(() -> new CustomException(ErrorCode.INTERNAL_SERVER_ERROR));

        try {
            // 외부 PG사 승인 API 호출 (동기 블로킹 호출)
            pgClient.confirm(command.paymentKey(), command.orderId(), command.amount());

            // DB 상태 업데이트 (PAID)
            payment.complete(command.paymentKey());

            // 6. Kafka 이벤트 객체 생성 (payment-common의 record 사용)
            PaymentCompletedEvent event = new PaymentCompletedEvent(
                    payment.getId(),
                    payment.getOrderId(),
                    payment.getAmount(),
                    payment.getType(),
                    payment.getMerchantId(),
                    LocalDateTime.now()
            );

            // 7. Kafka 이벤트 발행 호출 (내부적으로 비동기 처리 및 실패 시 FailedEvent 저장)
            eventProducer.sendPaymentCompletedEvent(event);

            log.info("결제 승인 및 이벤트 발행 완료: OrderId = {}", command.orderId());

        } catch (Exception e) {
            log.error("결제 승인 프로세스 중 에러 발생: OrderId = {}", command.orderId(), e);

            // PG 승인 실패 혹은 로직 에러 시 상태 변경
            payment.changeStatus(PaymentStatus.FAILED);

            // 공통 에러 핸들러로 예외 던짐
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
}
