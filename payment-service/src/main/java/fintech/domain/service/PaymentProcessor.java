package fintech.domain.service;

import java.math.BigDecimal;

public interface PaymentProcessor {

    // 실 결제 승인 요청
    String pay(BigDecimal amount, String orderId);
}
