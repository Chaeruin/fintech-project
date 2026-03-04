package fintech.global.exception;

import lombok.Builder;

@Builder
public record ErrorResponse(
        String code,
        String message,
        int status
) {
}
