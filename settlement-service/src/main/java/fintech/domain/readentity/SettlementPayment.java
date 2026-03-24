package fintech.domain.readentity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "settlement_payments",
        uniqueConstraints = @UniqueConstraint(name = "uk_order_id", columnNames = "orderId"))
public class SettlementPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String orderId;

    private String transactionKey;

    private BigDecimal amount;

    private String status;

    private LocalDateTime createdAt;

    @Builder
    public SettlementPayment(String orderId, String transactionKey,
                             BigDecimal amount, String status,
                             LocalDateTime createdAt) {
        this.orderId = orderId;
        this.transactionKey = transactionKey;
        this.amount = amount;
        this.status = status;
        this.createdAt = createdAt;
    }
}