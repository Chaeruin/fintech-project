package fintech.infra.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "failed_events")
public class FailedEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String topic;
    private String eventKey;

    @Column(columnDefinition = "TEXT")
    private String payload;

    private String errorMessage;    // 실패 원인
    private int retryCount = 0;     // 재시도 횟수

    private LocalDateTime createdAt;

    public FailedEvent(String topic, String eventKey, String payload, String errorMessage) {
        this.topic = topic;
        this.eventKey = eventKey;
        this.payload = payload;
        this.errorMessage = errorMessage;
        this.createdAt = LocalDateTime.now();
    }
}
