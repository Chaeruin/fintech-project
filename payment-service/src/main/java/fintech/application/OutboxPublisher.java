package fintech.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import fintech.domain.entity.OutboxEvent;
import fintech.domain.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OutboxPublisher {

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public void publish(String topic, String key, Object event) {
        try {
            String payload = objectMapper.writeValueAsString(event);

            outboxRepository.save(
                    new OutboxEvent(topic, key, "PAYMENT", null, payload));

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}