package fintech.infra.web;


import lombok.Builder;

@Builder
public record GatewayResponse(
        boolean success,
        String pgConfirmId,
        String message
) {

    public static GatewayResponse success(String pgConfirmId) {
        return GatewayResponse.builder()
                .success(true)
                .pgConfirmId(pgConfirmId)
                .message("Success")
                .build();
    }

    public static GatewayResponse fail(String message) {
        return GatewayResponse.builder()
                .success(false)
                .pgConfirmId(null)
                .message(message)
                .build();
    }
}