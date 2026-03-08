package fintech.infra.pg.dto;

import java.math.BigDecimal;
import lombok.Builder;

@Builder
public record TossTransactionResponse(
        String paymentKey,
        String orderId,
        String status,         // READY, IN_PROGRESS, DONE, CANCELED 등
        BigDecimal amount,
        String approvedAt      // ISO 8601 형식 (ex: 2024-01-01T00:00:00+09:00)
) {
}
