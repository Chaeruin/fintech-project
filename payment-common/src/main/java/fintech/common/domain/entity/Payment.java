package fintech.common.domain.entity;

import fintech.common.domain.enums.PaymentStatus;
import fintech.common.domain.enums.PaymentType;
import fintech.common.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "payments", indexes = {
        @Index(name = "idx_payment_order_id", columnList = "orderId")
})
public class Payment extends BaseEntity {

    // 가맹점 주문번호
    @Column(nullable = false, unique = true)
    private String orderId;

    // 가맹점 번호
    @Column(nullable = false, unique = true)
    private String merchantId;

    @Column(nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    // PG사 거래 키
    private String transactionKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentType type;

    public void complete(String transactionKey) {
        this.status = PaymentStatus.PAID;
        this.transactionKey = transactionKey;
    }

    public void changeStatus(PaymentStatus status) {
        this.status = status;
    }
}
