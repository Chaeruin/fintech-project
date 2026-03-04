package fintech.domain.repository;

import fintech.common.domain.entity.Payment;
import java.util.Optional;

public interface PaymentRepository {

    Payment save(Payment payment);
    Optional<Payment> findByOrderId(String orderId);
    Optional<Payment> findById(Long id);

}
