package fintech.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record PaymentEvent (
    String orderId,
    String merchantId,
    BigDecimal amount,
    String paymentKey,
    String status,
    LocalDateTime createdAt
){}
