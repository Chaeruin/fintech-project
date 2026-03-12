package fintech.domain.entity;

import fintech.domain.enums.PaymentStatus;
import fintech.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class PaymentHistory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id")
    // 어떤 결제 이력을 가지는지 단방향 매핑
    private Payment payment;

    // 변경 전 상태
    @Enumerated(EnumType.STRING)
    private PaymentStatus previousStatus;

    // 변경 후 상태
    @Enumerated(EnumType.STRING)
    private PaymentStatus currentStatus;

    // 상태 변경 사유 (예: "사용자 결제 완료", "관리자 강제 취소")
    private String reason;
}