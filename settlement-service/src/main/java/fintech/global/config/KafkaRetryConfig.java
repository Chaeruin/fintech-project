package fintech.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaRetryConfig {

    @Bean
    public DefaultErrorHandler errorHandler(KafkaOperations<Object, Object> template) {
        // 3번 재시도, 간격은 2초
        FixedBackOff backOff = new FixedBackOff(2000L, 3);

        // 재시도 끝에 실패 시 DLQ로 전송하는 DeadLetterPublishingRecoverer
        return new DefaultErrorHandler(new DeadLetterPublishingRecoverer(template), backOff);
    }
}
