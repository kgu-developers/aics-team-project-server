package kgu.developers.domain.teammessage.domain;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class TeamMessage {

    private Long id;
    private Long threadId;
    private String senderId;
    private String message;
    private TeamMessageRelatedType relatedType;
    private Long relatedId;
    private boolean important;
    private boolean read;
    private LocalDateTime createdAt;

    public static TeamMessage create(Long threadId, String senderId, TeamMessageRelatedType relatedType,
                                      Long relatedId, String message) {
        return TeamMessage.builder()
            .threadId(threadId)
            .senderId(senderId)
            .relatedType(relatedType != null ? relatedType : TeamMessageRelatedType.GENERAL)
            .relatedId(relatedId)
            .message(message)
            .important(false)
            .read(false)
            .build();
    }

    public void updateImportant(boolean important) {
        this.important = important;
    }

    public void markAsRead() {
        this.read = true;
    }
}
