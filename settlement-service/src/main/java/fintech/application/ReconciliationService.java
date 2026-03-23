package fintech.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import fintech.dto.PaymentEvent;
import fintech.infra.alert.AlertService;
import fintech.infra.persistence.SettlementJpaRepository;
import fintech.infra.persistence.entity.FailedEvent;
import fintech.infra.persistence.repository.FailedEventJpaRepository;
import fintech.infra.persistence.repository.PaymentJpaRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReconciliationService {

    private final PaymentJpaRepository paymentRepository;
    private final SettlementJpaRepository settlementRepository;
    private final FailedEventJpaRepository failedEventRepository;
    private final AlertService alertService;
    private final SettlementService settlementService;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public void reconcileDaily(LocalDate date) {
        log.info("[대사 시작] 대상 일자: {}", date);

        BigDecimal totalPaymentAmount = paymentRepository.sumAmountByDateAndStatus(date, "PAID")
                .orElse(BigDecimal.ZERO);
        List<String> paymentOrderIds = paymentRepository.findOrderIdsByDateAndStatus(date, "PAID");

        BigDecimal totalSettledAmount = settlementRepository.sumTotalAmountByDate(date)
                .orElse(BigDecimal.ZERO);
        List<String> settledOrderIds = settlementRepository.findOrderIdsByDate(date);

        BigDecimal diff = totalPaymentAmount.subtract(totalSettledAmount);

        if (diff.compareTo(BigDecimal.ZERO) == 0 && paymentOrderIds.size() == settledOrderIds.size()) {
            log.info("[대사 성공] 데이터 일치. 금액: {}", totalPaymentAmount);
        } else {
            List<String> missingIds = findMissingOrderIds(paymentOrderIds, settledOrderIds);
            traceAndRetry(date, missingIds, totalPaymentAmount, totalSettledAmount, diff);
        }
    }

    private List<String> findMissingOrderIds(List<String> paymentIds, List<String> settledIds) {
        Set<String> settledSet = new HashSet<>(settledIds);
        return paymentIds.stream()
                .filter(id -> !settledSet.contains(id))
                .collect(Collectors.toList());
    }

    private void traceAndRetry(LocalDate date, List<String> missingIds, BigDecimal target, BigDecimal actual, BigDecimal diff) {
        // 실패 기록 확인
        List<FailedEvent> failedEvents = failedEventRepository.findAllByOrderIdIn(missingIds);

        // 알림 발송
        sendDiscrepancyAlert(date, missingIds, failedEvents, target, actual, diff);

        // 3. 자동 재처리 로직 (실패 기록이 있는 건들에 대해)
        if (!failedEvents.isEmpty()) {
            log.info("[재처리] {}건의 누락 데이터 재처리 시도 중...", failedEvents.size());
            for (FailedEvent event : failedEvents) {
                try {
                    PaymentEvent paymentEvent = objectMapper.readValue(event.getPayload(), PaymentEvent.class);
                    // 실제 정산 처리 서비스 호출
                    settlementService.processSettlement(paymentEvent);

                    // 성공 시 실패 기록 삭제 또는 상태 변경
                    failedEventRepository.delete(event);
                    log.info("[재처리 성공] OrderId: {}", event.getId());
                } catch (Exception e) {
                    log.error("[재처리 실패] OrderId: {} - 사유: {}", event.getId(), e.getMessage());
                }
            }
        }
    }

    private void sendDiscrepancyAlert(LocalDate date, List<String> missingIds, List<FailedEvent> failedEvents, BigDecimal target, BigDecimal actual, BigDecimal diff) {
        String message = String.format(
                "*[대사 불일치 리포트]*\n일자: %s\n차액: %s (원장:%s / 정산:%s)\n누락ID: %s\n실패기록 발견: %d건",
                date, diff, target, actual, missingIds, failedEvents.size()
        );
        alertService.sendAlert(message);
    }
}
