package fintech.global.config;

import fintech.event.PaymentCompletedEvent;
import fintech.infra.kafka.KafkaRebalanceListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.CooperativeStickyAssignor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.VirtualThreadTaskExecutor;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.util.backoff.ExponentialBackOff;
import org.springframework.util.backoff.FixedBackOff;

@Slf4j
@EnableKafka
@Configuration
@RequiredArgsConstructor
public class KafkaConsumerConfig {

    private final KafkaRebalanceListener rebalanceListener;

    @Bean
    public ConsumerFactory<String, PaymentCompletedEvent> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "settlement-group");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest"); // 처음부터 읽기

        // 리밸런싱 최적화 - Incremental Cooperative Rebalancing 전략
        props.put(ConsumerConfig.PARTITION_ASSIGNMENT_STRATEGY_CONFIG,
                List.of(CooperativeStickyAssignor.class.getName()));

        // JSON 역직렬화 설정
        JsonDeserializer<PaymentCompletedEvent> deserializer = new JsonDeserializer<>(PaymentCompletedEvent.class);
        deserializer.setRemoveTypeHeaders(false);
        deserializer.addTrustedPackages("*");
        deserializer.setUseTypeMapperForKey(true);

        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), deserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, PaymentCompletedEvent> kafkaListenerContainerFactory(
            ConsumerFactory<String, PaymentCompletedEvent> consumerFactory,
            DefaultErrorHandler errorHandler) {

        ConcurrentKafkaListenerContainerFactory<String, PaymentCompletedEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setCommonErrorHandler(errorHandler); // 에러 핸들러 등록

        // ✅ Rebalance Listener 연결
        factory.getContainerProperties().setConsumerRebalanceListener(rebalanceListener);

        // 성능 튜닝 : 12개 파티션과 1:1 매핑을 위한 Concurrency 설정
        factory.setConcurrency(12);

        // 성능 튜닝 : Java 21 가상 스레드(Virtual Threads) 적용
        // 정산 DB 저장 시 발생하는 I/O 대기 시간 동안 스레드 효율을 극대화
        factory.getContainerProperties().setListenerTaskExecutor(
                new VirtualThreadTaskExecutor("settlement-vt-"));

        // 성능 튜닝 : 레코드 단위 Ack 설정 (세밀한 오프셋 관리)
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);

        return factory;
    }

    // 재시도 및 DLT 설정 핸들러
    @Bean
    public DefaultErrorHandler errorHandler(KafkaTemplate<String, Object> kafkaTemplate) {
        // 실패 시 DLT로 메시지 전송 설정
        // default : "원래토픽명.DLT"로 전송됨 (payment-completed.DLT)
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate);

        // 지수 백오프(Exponential BackOff) 적용: 3초 시작, 2.0 배수로 최대 3회 재시도
        //        // 무조건적인 반복보다 서버와 네트워크에 가해지는 부담을 줄입니다.
        ExponentialBackOff backOff = new ExponentialBackOff(3000L, 2.0);

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, backOff);

        // 에러 로그 출력
        errorHandler.setRetryListeners((record, ex, deliveryAttempt) -> {
            log.warn("정산 이벤트 처리 실패, 재시도 중... 시도 횟수: {}, Error: {}", deliveryAttempt, ex.getMessage());
        });

        return errorHandler;
    }
}
