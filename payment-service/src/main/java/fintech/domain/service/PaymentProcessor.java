package fintech.domain.service;

public interface PaymentProcessor {

    // 실 결제 승인 요청
    String pay(Long amount, String orderId);
}
