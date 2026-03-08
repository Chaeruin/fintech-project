package fintech.infra.pg;

import fintech.domain.service.PaymentProcessor;
import fintech.infra.portone.PortOnePgClient;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PortOnePaymentProcessor implements PaymentProcessor {
    private final PortOnePgClient portOnePgClient;

    @Override
    public String pay(BigDecimal amount, String orderId) {
        portOnePgClient.confirm(orderId, orderId, amount);
        return "PORTONE_CONFIRM_ID_" + orderId;
    }

    @Override
    public void cancel(String pgConfirmId, String reason) {
        // 실제 외부 API 호출 (Toss 취소 API)
        portOnePgClient.cancel(pgConfirmId, reason);
    }

    @Override
    public String getPgType() {
        return "";
    }
}