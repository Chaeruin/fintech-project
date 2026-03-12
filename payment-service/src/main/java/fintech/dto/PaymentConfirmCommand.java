package fintech.dto;

import java.math.BigDecimal;

public record PaymentConfirmCommand(
        String orderId,
        BigDecimal amount,
        String paymentKey,
        String pgType       // TOSS or PORTONE
) {
}
