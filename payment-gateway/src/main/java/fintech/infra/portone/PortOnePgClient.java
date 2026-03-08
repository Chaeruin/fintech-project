package fintech.infra.portone;


import fintech.common.PgClient;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@RequiredArgsConstructor
public class PortOnePgClient implements PgClient {

    @Qualifier("portonePaymentWebClient")
    private final WebClient webClient;

    @Override
    public void confirm(String paymentKey, String orderId, BigDecimal amount) {
        // 포트원 스펙에 맞는 엔드포인트와 데이터 구조
        webClient.post()
                .uri("/payments/confirm")
                .bodyValue(new PortOneConfirmRequest(paymentKey, amount))
                .retrieve()
                .bodyToMono(Void.class)
                .block();
    }

    @Override
    public void cancel(String pgConfirmId, String reason) {

    }

    @Override
    public String getPgType() { return "PORTONE"; }

    private record PortOneConfirmRequest(String imp_uid, BigDecimal amount) {}
}
