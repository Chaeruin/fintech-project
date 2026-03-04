package fintech.common.domain.dto.event;

import java.time.LocalDateTime;

public record PaymentFailedEvent(
        String orderId,
        String errorCode,
        String errorMessage,
        LocalDateTime failedAt
) { }
