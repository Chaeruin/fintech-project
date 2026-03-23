package fintech.infra.persistence.repository;

import fintech.domain.entity.Payment;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
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

    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.payDate = :date AND p.status = :status")
    Optional<BigDecimal> sumAmountByDateAndStatus(LocalDate date, String status);

    @Query("SELECT p.orderId FROM Payment p WHERE p.payDate = :date AND p.status = :status")
    List<String> findOrderIdsByDateAndStatus(LocalDate date, String status);
}
