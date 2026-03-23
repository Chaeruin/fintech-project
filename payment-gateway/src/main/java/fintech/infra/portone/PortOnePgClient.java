package fintech.infra.portone;


import fintech.infra.pg.PgClient;
import fintech.dto.PgTransactionDto;
import fintech.global.exception.CustomException;
import fintech.global.exception.ErrorCode;
import fintech.infra.pg.dto.PortOneResponse;
import fintech.infra.toss.TossPgClient.TossConfirmRequest;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
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
public class PortOnePgClient implements PgClient {

    @Qualifier("portonePaymentWebClient")
    private final WebClient webClient;

    // 결제 승인 로직
    @Override
    @Retry(name = "pgService", fallbackMethod = "fallbackConfirm")
    @CircuitBreaker(name = "pgService")
    public void confirm(String paymentKey, String orderId, BigDecimal amount) {
        log.info("[PG 승인 요청] OrderId: {}, Amount: {}", orderId, amount);

        webClient.post()
                .uri("/api/v1/payments/confirm")
                .bodyValue(new PortOneConfirmRequest(paymentKey, amount))
                .retrieve()
                .onStatus(HttpStatusCode::isError, response ->
                        response.bodyToMono(String.class).flatMap(errorBody ->
                                Mono.error(new RuntimeException("PG API Error: " + errorBody))))
                .bodyToMono(Void.class)
                .block(); // 동기 방식으로 대기
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
    public List<PgTransactionDto> fetchSuccessHistory(LocalDate date) {
        // 포트원 API: 결제 완료 상태인 내역을 기간별로 조회
        long from = date.atStartOfDay(ZoneId.systemDefault()).toEpochSecond();
        long to = date.atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toEpochSecond();

        return webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/payments/status/paid")
                        .queryParam("from", from)
                        .queryParam("to", to)
                        .build())
                .header("Authorization", "PortOne_Access_Token") // 실제 토큰 로직 필요
                .retrieve()
                .bodyToFlux(PortOneResponse.class)
                .map(res -> PgTransactionDto.builder()
                        .pgId(res.imp_uid())
                        .orderId(res.merchant_uid())
                        .amount(res.amount())
                        .status(res.status().toUpperCase())
                        .approvedAt(LocalDateTime.ofInstant(Instant.ofEpochSecond(res.paid_at()), ZoneId.systemDefault()))
                        .build())
                .collectList()
                .block();
    }

    @Override
    public String getPgType() { return "PORTONE"; }

    // Fallback 메서드
    public void fallbackConfirm(String paymentKey, String orderId, BigDecimal amount, Throwable t) {
        log.error("PortOne PG사 서킷 오픈 혹은 타임아웃 발생: {}", t.getMessage());
        throw new CustomException(ErrorCode.PG_TEMPORARY_UNAVAILABLE);
    }

    private record PortOneConfirmRequest(String imp_uid, BigDecimal amount) {}
}
