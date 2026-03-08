package fintech.infra.toss;


import fintech.common.PgClient;
import fintech.common.global.exception.CustomException;
import java.math.BigDecimal;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import fintech.common.global.exception.ErrorCode;

@Component
@RequiredArgsConstructor
@Slf4j
public class TossPgClient implements PgClient {

    @Qualifier("tossPaymentWebClient")
    private final WebClient webClient;

    // 결제 승인 로직
    @Override
    public void confirm(String paymentKey, String orderId, BigDecimal amount) {
        webClient.post()
                .uri("/v1/payments/confirm")
                .bodyValue(new TossConfirmRequest(paymentKey, orderId, amount))
                .retrieve()
                .bodyToMono(Void.class)
                .block();
    }

    // 결제 취소 로직
    @Override
    public void cancel(String pgConfirmId, String reason) {
        webClient.post()
                .uri(uriBuilder -> uriBuilder.path("/v1/payments/{pgConfirmId}/cancel").build(pgConfirmId))
                .bodyValue(Map.of("cancelReason", reason))
                .retrieve()
                .onStatus(HttpStatusCode::isError, res -> {
                    log.error("[PG 취소 실패] pgConfirmId: {}", pgConfirmId);
                    return Mono.error(new CustomException(ErrorCode.PAYMENT_CANCEL_FAILED));
                })
                .bodyToMono(Void.class)
                .block();
    }

    @Override
    public String getPgType() {
        return "TOSS";
    }

    private record TossConfirmRequest(String paymentKey, String orderId, BigDecimal amount) {}
}
