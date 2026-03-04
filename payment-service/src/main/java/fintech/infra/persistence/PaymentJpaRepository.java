package fintech.infra.persistence;

import fintech.common.domain.entity.Payment;
import fintech.domain.repository.PaymentRepository;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentJpaRepository extends JpaRepository<Payment, Long>, PaymentRepository {

    @Override
    Optional<Payment> findByOrderId(String orderId);
}
