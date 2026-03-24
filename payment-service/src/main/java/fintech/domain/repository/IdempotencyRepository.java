package fintech.domain.repository;

import fintech.domain.entity.IdempotencyKey;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IdempotencyRepository extends JpaRepository<IdempotencyKey, Long> {

    boolean existsByIdempotencyKey(String idempotencyKey);
}