package fintech.global.error;

import lombok.Builder;

@Builder
public record ErrorResponse(
        String code,
        String message,
        int status
) {
    public static ErrorResponse of(String code, String message, int status) {
        return new ErrorResponse(code, message, status);
    }
}
