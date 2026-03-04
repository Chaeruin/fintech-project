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
    public String pay(Long amount, String orderId) {
        tossPgClient.confirm(orderId, orderId, BigDecimal.valueOf(amount));
        return "TOSS_CONFIRM_ID_" + orderId; // 성공 시 식별자 반환
    }
}
