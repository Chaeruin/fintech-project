package fintech.common.domain.entity;


import fintech.common.domain.enums.SettlementStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "settlements", indexes = {
        @Index(name = "idx_merchant_date", columnList = "merchantId, settlementDate")
})
public class Settlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String merchantId;

    @Column(nullable = false)
    private BigDecimal totalAmount;

    @Column(nullable = false)
    private BigDecimal totalFee;

    @Column(nullable = false)
    private BigDecimal settlementAmount; // 실제 지급액 (Total - Fee)

    @Column(nullable = false)
    private LocalDateTime settlementDate; // 정산 기준일 (보통 결제일 + 1일)

    @Enumerated(EnumType.STRING)
    private SettlementStatus status;

    @Builder
    public Settlement(String merchantId, BigDecimal totalAmount, BigDecimal totalFee,
                      BigDecimal settlementAmount, LocalDateTime settlementDate) {
        this.merchantId = merchantId;
        this.totalAmount = totalAmount;
        this.totalFee = totalFee;
        this.settlementAmount = settlementAmount;
        this.settlementDate = settlementDate;
        this.status = SettlementStatus.READY;   // 초기값은 항상 대기
    }

    // 정산 완료 처리
    public void complete() {
        this.status = SettlementStatus.COMPLETED;
    }

    public void updateAmount(BigDecimal amount, BigDecimal fee, BigDecimal settlementAmount) {
        this.totalAmount = this.totalAmount.add(amount);
        this.totalFee = this.totalFee.add(fee);
        this.settlementAmount = this.settlementAmount.add(settlementAmount);
    }
}
