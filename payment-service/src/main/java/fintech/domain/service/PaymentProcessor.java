package fintech.domain.service;

import java.math.BigDecimal;

public interface PaymentProcessor {

    // 실 결제 승인 요청
    String pay(BigDecimal amount, String orderId);

    // 보상 트랜잭션을 위한 결제 취소 메서드 추가
    void cancel(String pgConfirmId, String reason);

    String getPgType();
}
