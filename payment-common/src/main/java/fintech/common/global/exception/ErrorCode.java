package fintech.common.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 공통 에러
    INVALID_INPUT_VALUE(400, "C001", "잘못된 입력 값입니다."),
    METHOD_NOT_ALLOWED(405, "C002", "허용되지 않은 메서드입니다."),
    INTERNAL_SERVER_ERROR(500, "C003", "서버 내부 오류입니다."),

    // 결제 관련 (P로 시작)
    PAYMENT_NOT_FOUND(404, "P001", "결제 내역을 찾을 수 없습니다."),
    INVALID_PAYMENT_AMOUNT(400, "P002", "결제 금액이 일치하지 않습니다."),
    ALREADY_PAID(400, "P003", "이미 완료된 결제건입니다."),
    PAYMENT_CANCEL_FAILED(400, "P004", "결제 취소에 실패했습니다."),

    // 외부 PG 연동 관련 (E로 시작)
    EXTERNAL_PG_ERROR(502, "E001", "PG사 통신 중 오류가 발생했습니다.");

    private final int status; // HTTP 상태 코드
    private final String code; // 비즈니스 에러 코드
    private final String message; // 사용자 메시지

}
