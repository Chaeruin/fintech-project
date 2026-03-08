package fintech.common.domain.dto.event;

import fintech.common.domain.enums.PaymentType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record PaymentCompletedEvent(
        String paymentId,             // 결제 고유 ID
        String orderId,             // 주문 번호
        BigDecimal amount,          // 결제 금액
        PaymentType paymentType,    // 결제 수단
        String merchantId,          // 가맹점 ID (정산 주체)
        LocalDateTime completedAt   // 결제 완료 시각
) {}
