package kgu.developers.domain.notification.domain;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class NotificationOutbox {

    private Long id;
    private String userId;
    private NotificationType type;
    private Long sourceId;
    private String title;
    private String message;
    private String link;
    private OutboxStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime processedAt;
    private int retryCount;
    private String errorMessage;

    public static NotificationOutbox create(
        String userId,
        NotificationType type,
        Long sourceId,
        String title,
        String message,
        String link
    ) {
        return NotificationOutbox.builder()
            .userId(userId)
            .type(type)
            .sourceId(sourceId)
            .title(title)
            .message(message)
            .link(link)
            .status(OutboxStatus.PENDING)
            .createdAt(LocalDateTime.now())
            .retryCount(0)
            .build();
    }

    public void markAsProcessed() {
        this.status = OutboxStatus.PROCESSED;
        this.processedAt = LocalDateTime.now();
    }

    public void markAsFailed(String errorMessage) {
        this.status = OutboxStatus.FAILED;
        this.errorMessage = errorMessage;
        this.retryCount++;
    }

    public void markAsPending() {
        this.status = OutboxStatus.PENDING;
        this.errorMessage = null;
    }

    public boolean canRetry(int maxRetries) {
        return this.status == OutboxStatus.FAILED && this.retryCount < maxRetries;
    }
}