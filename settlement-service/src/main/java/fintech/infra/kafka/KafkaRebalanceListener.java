package fintech.infra.kafka;

import java.util.Collection;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.common.TopicPartition;
import org.springframework.kafka.listener.ConsumerAwareRebalanceListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class KafkaRebalanceListener implements ConsumerAwareRebalanceListener {

    // Rebalance 직전 (partition 회수)
    @Override
    public void onPartitionsRevokedBeforeCommit(Consumer<?, ?> consumer,
                                                Collection<TopicPartition> partitions) {

        log.warn("[Rebalance] 파티션 할당 취소 : {}", partitions);

        // 필요 시 offset commit 가능 (지금은 RECORD ack라 필수 아님)
    }

    // Rebalance 이후 (partition 할당)
    @Override
    public void onPartitionsAssigned(Consumer<?, ?> consumer,
                                     Collection<TopicPartition> partitions) {

        log.info("[Rebalance] 파티션 할당됨 : {}", partitions);
    }
}