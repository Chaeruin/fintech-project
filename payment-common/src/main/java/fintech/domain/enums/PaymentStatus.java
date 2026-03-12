package fintech.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PaymentStatus {

    READY("결제 준비"),
    IN_PROGRESS("결제 진행 중"),
    PAID("결제 완료"),
    FAILED("결제 실패"),
    CANCELLED("결제 취소"),
    SETTLEMENT_READY("정산 대기"),
    SETTLEMENT_COMPLETED("정산 완료");

    private final String description;
}
