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
public class OutboxEvent {

    @Id
    @GeneratedValue
    private Long id;

    private String topic;
    private String eventKey;

    private String aggregateType; // "PAYMENT"
    private Long aggregateId;     // 결제 PK
    private String payload;

    private String eventType;     // "INIT" -> "PUBLISHED" 로 변경

    public OutboxEvent(String topic, String eventKey, String aggregateType, Long aggregateId, String payload) {
        this.topic = topic;
        this.eventKey = eventKey;
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.payload = payload;
        this.eventType = "INIT";
    }

    public void markPublished() {
        this.eventType = "PUBLISHED";
    }
}
