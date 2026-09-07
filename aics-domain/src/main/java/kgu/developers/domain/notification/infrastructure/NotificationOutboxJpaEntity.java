package kgu.developers.domain.notification.infrastructure;

import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import kgu.developers.domain.notification.domain.NotificationOutbox;
import kgu.developers.domain.notification.domain.NotificationType;
import kgu.developers.domain.notification.domain.OutboxStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "\"notification_outbox\"")
@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = PROTECTED)
public class NotificationOutboxJpaEntity {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, length = 20)
    private String userId;

    @Column(nullable = false, length = 50)
    @Enumerated(STRING)
    private NotificationType type;

    @Column(name = "source_id")
    private Long sourceId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String message;

    private String link;

    @Column(nullable = false, length = 20)
    @Enumerated(STRING)
    private OutboxStatus status;

    @Column(name = "created_at", nullable = false)
    private java.time.LocalDateTime createdAt;

    @Column(name = "processed_at")
    private java.time.LocalDateTime processedAt;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    public NotificationOutbox toDomain() {
        return NotificationOutbox.builder()
            .id(this.id)
            .userId(this.userId)
            .type(this.type)
            .sourceId(this.sourceId)
            .title(this.title)
            .message(this.message)
            .link(this.link)
            .status(this.status)
            .createdAt(this.createdAt)
            .processedAt(this.processedAt)
            .retryCount(this.retryCount)
            .errorMessage(this.errorMessage)
            .build();
    }

    public static NotificationOutboxJpaEntity toEntity(NotificationOutbox domain) {
        return NotificationOutboxJpaEntity.builder()
            .id(domain.getId())
            .userId(domain.getUserId())
            .type(domain.getType())
            .sourceId(domain.getSourceId())
            .title(domain.getTitle())
            .message(domain.getMessage())
            .link(domain.getLink())
            .status(domain.getStatus())
            .createdAt(domain.getCreatedAt())
            .processedAt(domain.getProcessedAt())
            .retryCount(domain.getRetryCount())
            .errorMessage(domain.getErrorMessage())
            .build();
    }
}