package fintech.common;

import fintech.common.domain.dto.PgTransactionDto;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface PgClient {

    // 외부 PG 사에 결제 승인 요청
    void confirm(String paymentKey, String orderId, BigDecimal amount);

    // 실제 외부 API 호출 (Toss 취소 API)
    void cancel(String pgConfirmId, String reason);

    // 3자 대조를 위해 특정 날짜의 성공 내역을 가져오는 메서드 추가
    List<PgTransactionDto> fetchSuccessHistory(LocalDate date);

    // 지원 PG 타입 반환
    String getPgType();
}
