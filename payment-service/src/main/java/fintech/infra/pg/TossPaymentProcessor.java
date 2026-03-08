package fintech.infra.pg;

import fintech.domain.service.PaymentProcessor;
import fintech.infra.toss.TossPgClient;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TossPaymentProcessor implements PaymentProcessor {
    private final TossPgClient tossPgClient;

    @Override
    public String pay(BigDecimal amount, String orderId) {
        tossPgClient.confirm(orderId, orderId, amount);
        return "TOSS_CONFIRM_ID_" + orderId; // 성공 시 식별자 반환
    }

    @Override
    public void cancel(String pgConfirmId, String reason) {
        // 실제 외부 API 호출 (Toss 취소 API)
        tossPgClient.cancel(pgConfirmId, reason);
    }

    @Override
    public String getPgType() {
        return tossPgClient.getPgType();
    }
}
