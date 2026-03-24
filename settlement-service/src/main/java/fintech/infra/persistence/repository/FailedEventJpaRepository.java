package fintech.infra.persistence.repository;

import fintech.infra.persistence.entity.FailedEvent;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FailedEventJpaRepository extends JpaRepository<FailedEvent, Long> {
    List<FailedEvent> findAllByOrderIdIn(List<String> orderIds);

    List<FailedEvent> findAllByOrderIdInAndStatus(List<String> orderIds, String status);

    List<FailedEvent> findTop10ByStatusOrderByCreatedAtAsc(String status);

    List<FailedEvent> findTop100ByRetryCountLessThan(int retryCount);

}
