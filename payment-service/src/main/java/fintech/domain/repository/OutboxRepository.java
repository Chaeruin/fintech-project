package fintech.domain.repository;

import fintech.domain.entity.OutboxEvent;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxRepository extends JpaRepository<OutboxEvent, Long> {

    List<OutboxEvent> findAllByAggregateTypeAndEventType(String aggregateType, String eventType);
}
