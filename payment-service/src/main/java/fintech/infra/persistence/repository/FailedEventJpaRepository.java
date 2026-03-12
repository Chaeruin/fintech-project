package fintech.infra.persistence.repository;

import fintech.infra.persistence.entity.FailedEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FailedEventJpaRepository extends JpaRepository<FailedEvent, Long> {
}
