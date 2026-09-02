package kgu.developers.domain.notification.domain;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class Notification {

    private Long id;
    private String userId;
    private NotificationType type;
    private Long sourceId;
    private String title;
    private String message;
    private String link;
    private boolean isRead;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static Notification create(
        String userId,
        NotificationType type,
        Long sourceId,
        String title,
        String message,
        String link
    ) {
        return Notification.builder()
            .userId(userId)
            .type(type)
            .sourceId(sourceId)
            .title(title)
            .message(message)
            .link(link)
            .isRead(false)
            .build();
    }
}
