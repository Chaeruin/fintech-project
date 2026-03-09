package fintech.infra.persistence;

import fintech.common.domain.entity.Payment;
import fintech.domain.repository.PaymentRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentJpaRepository extends JpaRepository<Payment, Long> {

    // 날짜 범위(Between)와 결제 상태(Status)로 리스트 조회
    List<Payment> findAllByCreatedAtBetweenAndStatus(
            LocalDateTime start,
            LocalDateTime end,
            String status
    );

    Optional<Payment> findByOrderId(String orderId);

    Optional<Payment> findById(Long id);
}
