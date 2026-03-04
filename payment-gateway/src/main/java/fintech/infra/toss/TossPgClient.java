package fintech.infra.toss;


import fintech.common.PgClient;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@RequiredArgsConstructor
public class TossPgClient implements PgClient {

    @Qualifier("tossPaymentWebClient")
    private final WebClient webClient;

    @Override
    public void confirm(String paymentKey, String orderId, BigDecimal amount) {
        webClient.post()
                .uri("/v1/payments/confirm")
                .bodyValue(new TossConfirmRequest(paymentKey, orderId, amount))
                .retrieve()
                .bodyToMono(Void.class)
                .block();
    }

    @Override
    public String getPgType() {
        return "TOSS";
    }

    private record TossConfirmRequest(String paymentKey, String orderId, BigDecimal amount) {}
}
