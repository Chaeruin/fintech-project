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
    public String pay(Long amount, String orderId) {
        portOnePgClient.confirm(orderId, orderId, BigDecimal.valueOf(amount));
        return "PORTONE_CONFIRM_ID_" + orderId;
    }
}