package fintech.domain.repository;

import fintech.domain.entity.IdempotencyKey;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IdempotencyRepository extends JpaRepository<IdempotencyKey, Long> {

    Optional<IdempotencyKey> findByIdempotencyKey(String key);

    boolean existsByIdempotencyKey(String idempotencyKey);
}