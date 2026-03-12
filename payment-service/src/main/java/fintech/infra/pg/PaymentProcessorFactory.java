package fintech.infra.pg;

import fintech.global.exception.CustomException;
import fintech.global.exception.ErrorCode;
import fintech.domain.service.PaymentProcessor;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class PaymentProcessorFactory {

    private final Map<String, PaymentProcessor> processors = new ConcurrentHashMap<>();

    public PaymentProcessorFactory(PortOnePaymentProcessor portOne, TossPaymentProcessor toss) {
        processors.put("PORTONE", portOne);
        processors.put("TOSS", toss);
    }

    public PaymentProcessor getProcessor(String pgType) {
        PaymentProcessor processor = processors.get(pgType.toUpperCase());
        if (processor == null) {
            throw new CustomException(ErrorCode.EXTERNAL_PG_ERROR);
        }
        return processor;
    }
}
