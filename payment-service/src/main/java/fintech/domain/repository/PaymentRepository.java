package fintech.domain.repository;

import fintech.domain.entity.Payment;
import java.util.Optional;


public interface PaymentRepository {

    void save(Payment payment);
    Optional<Payment> findById(Long id);

}
