package fintech.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record PgTransactionDto(
        String pgId,         // PG사 승인 번호
        String orderId,      // 우리 주문 번호
        BigDecimal amount,   // 결제 금액
        String status,       // 결제 상태 (DONE, CANCELLED 등)
        LocalDateTime approvedAt
) {
}
