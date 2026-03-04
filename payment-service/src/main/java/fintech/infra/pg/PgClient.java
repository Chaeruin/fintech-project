package fintech.infra.pg;

import java.math.BigDecimal;

public interface PgClient {

    // 외부 PG 사에 결제 승인 요청
    void confirm(String paymentKey, String orderId, BigDecimal amount);

    // 지원 PG 타입 반환
    String getPgType();
}
