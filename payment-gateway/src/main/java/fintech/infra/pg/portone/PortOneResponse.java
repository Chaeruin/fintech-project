package fintech.infra.pg.portone;

import java.math.BigDecimal;
import lombok.Builder;

@Builder
public record PortOneResponse(
        String imp_uid,        // 포트원 고유 번호 (pgId)
        String merchant_uid,   // 우리 주문 번호 (orderId)
        String status,         // paid, cancelled 등
        BigDecimal amount,
        long paid_at           // UNIX Timestamp (seconds)
) {
}
