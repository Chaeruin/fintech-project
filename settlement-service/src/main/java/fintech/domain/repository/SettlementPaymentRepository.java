package fintech.domain.repository;

import fintech.domain.readentity.SettlementPayment;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SettlementPaymentRepository
        extends JpaRepository<SettlementPayment, Long> {

    boolean existsByOrderId(String orderId);

    List<SettlementPayment> findAllByCreatedAtBetweenAndStatus(
            LocalDateTime start, LocalDateTime end, String status);

    @Query("select coalesce(sum(p.amount), 0) from SettlementPayment p " +
            "where DATE(p.createdAt) = :date and p.status = :status")
    BigDecimal sumAmountByDateAndStatus(LocalDate date, String status);

    @Query("select p.orderId from SettlementPayment p " +
            "where DATE(p.createdAt) = :date and p.status = :status")
    List<String> findOrderIdsByDateAndStatus(LocalDate date, String status);
}