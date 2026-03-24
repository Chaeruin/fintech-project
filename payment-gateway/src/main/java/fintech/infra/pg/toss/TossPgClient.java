package fintech.infra.pg.toss;

import fintech.infra.pg.client.PgClient;
import fintech.dto.PgTransactionDto;
import fintech.global.exception.CustomException;
import fintech.global.exception.ErrorCode;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Base64; // 1. Base64 임포트 추가
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Slf4j
public class TossPgClient implements PgClient {

    @Qualifier("tossPaymentWebClient")
    private final WebClient webClient;

    @Override
    @Retry(name = "pgService", fallbackMethod = "fallbackConfirm")
    @CircuitBreaker(name = "pgService")
    public void confirm(String paymentKey, String orderId, BigDecimal amount) {
        log.info("[PG 승인 요청] OrderId: {}, Amount: {}", orderId, amount);

        webClient.post()
                .uri("/api/v1/payments/confirm")
                .bodyValue(new TossConfirmRequest(paymentKey, orderId, amount))
                .retrieve()
                .onStatus(HttpStatusCode::isError, response ->
                        response.bodyToMono(String.class).flatMap(errorBody ->
                                Mono.error(new RuntimeException("PG API Error: " + errorBody))))
                .bodyToMono(Void.class)
                .block(); // 동기 방식으로 대기
    }

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
    public List<PgTransactionDto> fetchSuccessHistory(LocalDate date) {
        String start = date.atStartOfDay().toString();
        String end = date.atTime(LocalTime.MAX).toString();

        return webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/v1/transactions")
                        .queryParam("startDate", start)
                        .queryParam("endDate", end)
                        .build())
                // 실제 운영시에는 Config에서 설정된 WebClient를 쓰므로 헤더 중복 여부 확인 필요
                .header("Authorization", "Basic " + Base64.getEncoder().encodeToString("TOSS_SECRET_KEY:".getBytes()))
                .retrieve()
                .bodyToFlux(TossTransactionResponse.class)
                .filter(res -> "DONE".equals(res.status()))
                .map(res -> PgTransactionDto.builder()
                        .pgId(res.paymentKey())
                        .orderId(res.orderId())
                        .status(res.status())
                        .amount(res.amount())
                        .approvedAt(OffsetDateTime.parse(res.approvedAt()).toLocalDateTime())
                        .build())
                .collectList()
                .block();
    }

    @Override
    public String getPgType() {
        return "TOSS";
    }

    public void fallbackConfirm(String paymentKey, String orderId, BigDecimal amount, Throwable t) {
        log.error("TOSS PG사 서킷 오픈 혹은 타임아웃 발생: {}", t.getMessage());
        throw new CustomException(ErrorCode.PG_TEMPORARY_UNAVAILABLE);
    }

    private record TossConfirmRequest(String paymentKey, String orderId, BigDecimal amount) {}
}