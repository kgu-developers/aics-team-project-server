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
import kgu.developers.common.domain.BaseTimeEntity;
import kgu.developers.domain.notification.domain.Notification;
import kgu.developers.domain.notification.domain.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "\"notification\"")
@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = PROTECTED)
public class NotificationJpaEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, length = 20)
    private String userId;

    @Column(nullable = false, length = 30)
    @Enumerated(STRING)
    private NotificationType type;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String message;

    private String link;

    @Column(name = "is_read", nullable = false)
    private boolean isRead;

    public Notification toDomain() {
        return Notification.builder()
            .id(this.id)
            .userId(this.userId)
            .type(this.type)
            .title(this.title)
            .message(this.message)
            .link(this.link)
            .isRead(this.isRead)
            .createdAt(this.getCreatedAt())
            .updatedAt(this.getUpdatedAt())
            .build();
    }

    public static NotificationJpaEntity toEntity(Notification domain) {
        return NotificationJpaEntity.builder()
            .id(domain.getId())
            .userId(domain.getUserId())
            .type(domain.getType())
            .title(domain.getTitle())
            .message(domain.getMessage())
            .link(domain.getLink())
            .isRead(domain.isRead())
            .build();
    }
}
