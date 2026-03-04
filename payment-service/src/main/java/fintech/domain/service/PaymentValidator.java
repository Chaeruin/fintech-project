package fintech.domain.service;

import fintech.common.domain.entity.Payment;
import fintech.common.domain.enums.PaymentStatus;
import fintech.common.global.exception.CustomException;
import fintech.common.global.exception.ErrorCode;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;

@Service
public class PaymentValidator {

    // 결제 승인 전 검증
    public void validateAction(Payment payment, BigDecimal requestAmount) {
        // 이미 완료된 결제인지 확인
        if (payment.getStatus() == PaymentStatus.PAID) {
            throw new CustomException(ErrorCode.ALREADY_PAID);
        }

        // 요청 금액과 DB에 기록된 금액이 일치하는지 확인 (데이터 정합성 체크 핵심)
        if (payment.getAmount().compareTo(requestAmount) != 0) {
            throw new CustomException(ErrorCode.INVALID_PAYMENT_AMOUNT);
        }
    }

    // 결제 취소 가능 여부 확인
    public void validateCancel(Payment payment) {
        if (payment.getStatus() != PaymentStatus.PAID) {
            throw new CustomException(ErrorCode.PAYMENT_NOT_FOUND);
        }
    }
}
