package fintech.domain.repository;

import fintech.common.domain.entity.Payment;
import fintech.infra.persistence.PaymentJpaRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class PaymentJpaRepositoryImpl implements PaymentRepository {

    private final PaymentJpaRepository paymentJpaRepository;

    @Override
    public void save(Payment payment) {
        paymentJpaRepository.save(payment);
    }

    @Override
    public Optional<Payment> findByOrderId(String orderId) {
        return paymentJpaRepository.findByOrderId(orderId);
    }

    @Override
    public Optional<Payment> findById(Long id) {
        return paymentJpaRepository.findById(id);
    }
}
