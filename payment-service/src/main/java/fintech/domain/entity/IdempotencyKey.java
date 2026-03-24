package fintech.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
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
public class IdempotencyKey {

    @Id
    @GeneratedValue
    private Long id;

    @Column(unique = true)
    private String idempotencyKey;

    private String responseBody;

    private Long paymentId;

    public IdempotencyKey(String idempotencyKey, Long paymentId) {
        this.idempotencyKey = idempotencyKey;
        this.responseBody = "SUCCESS";
        this.paymentId = paymentId;
    }
}