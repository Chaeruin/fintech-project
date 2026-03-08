package fintech.infra.toss;

import fintech.common.PgClient;
import fintech.common.domain.dto.PgTransactionDto;
import fintech.common.global.exception.CustomException;
import fintech.common.global.exception.ErrorCode;
import fintech.infra.pg.dto.TossTransactionResponse;
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
    public void confirm(String paymentKey, String orderId, BigDecimal amount) {
        webClient.post()
                .uri("/v1/payments/confirm")
                .bodyValue(new TossConfirmRequest(paymentKey, orderId, amount))
                .retrieve()
                .bodyToMono(Void.class)
                .block();
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
                // 2. 실제 운영시에는 Config에서 설정된 WebClient를 쓰므로 헤더 중복 여부 확인 필요
                .header("Authorization", "Basic " + Base64.getEncoder().encodeToString("TOSS_SECRET_KEY:".getBytes()))
                .retrieve()
                .bodyToFlux(TossTransactionResponse.class)
                .filter(res -> "DONE".equals(res.status()))
                .map(res -> PgTransactionDto.builder()
                        .pgId(res.paymentKey())
                        .orderId(res.orderId())
                        .status(res.status())
                        .amount(res.amount()) // 3. long -> BigDecimal 변환
                        .approvedAt(OffsetDateTime.parse(res.approvedAt()).toLocalDateTime()) // 4. 필드명 통일 (transactionAt)
                        .build())
                .collectList()
                .block();
    } // 5. 중괄호 위치 수정됨

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