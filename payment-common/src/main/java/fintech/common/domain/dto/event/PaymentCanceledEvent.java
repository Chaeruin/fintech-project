package fintech.common.domain.dto.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentCanceledEvent (
        Long paymentId,
        String orderId,
        BigDecimal cancelAmount,
        String reason,
        LocalDateTime canceledAt
) {}
