package fintech.job;


import fintech.infra.pg.PgClient;
import fintech.dto.PgTransactionDto;
import fintech.domain.entity.Payment;
import fintech.infra.alert.AlertProvider;
import fintech.infra.persistence.repository.PaymentJpaRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExternalReconciliationJob {
    private final PgClient pgClient;
    private final PaymentJpaRepository paymentRepository;
    private final AlertProvider alertProvider;

    @Scheduled(cron = "0 0 3 * * *")
    public void run() {
        // 어제 날짜 범위 설정
        LocalDateTime start = LocalDate.now().minusDays(1).atStartOfDay();
        LocalDateTime end = LocalDate.now().atStartOfDay();

        // PG사 실제 성공 내역 조회
        List<PgTransactionDto> pgSuccessList = pgClient.fetchSuccessHistory(start.toLocalDate());

        // 우리 원장(DB) 성공 내역 조회
        List<Payment> internalList = paymentRepository.findAllByCreatedAtBetweenAndStatus(start, end, "PAID");

        // 대조 로직
        // Case A: PG에는 있는데 우리 DB에는 없는 경우 (낙구 - 심각)
        pgSuccessList.forEach(pg -> {
            boolean exists = internalList.stream()
                    .anyMatch(p -> p.getTransactionKey().equals(pg.pgId()));
            if (!exists) {
                alertProvider.sendAlert("⚠️ 원장 누락 발견(낙구)! PG_ID: " + pg.pgId() + ", 주문번호: " + pg.orderId());
            }
        });

        // Case B: 우리 DB에는 PAID인데 PG에는 성공 내역이 없는 경우 (데이터 불일치 - 주의)
        internalList.forEach(p -> {
            boolean exists = pgSuccessList.stream()
                    .anyMatch(pg -> pg.pgId().equals(p.getTransactionKey()));
            if (!exists) {
                alertProvider.sendAlert("⚠️ PG 내역 부재 발견! 우리 원장에는 PAID이나 PG사에는 내역 없음. PG_ID: "
                        + p.getTransactionKey());
            }
        });
    }
}
