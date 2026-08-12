package kgu.developers.domain.teammessage.infrastructure;

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
import kgu.developers.domain.teammessage.domain.TeamMessage;
import kgu.developers.domain.teammessage.domain.TeamMessageRelatedType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "\"team_message\"")
@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = PROTECTED)
public class TeamMessageJpaEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @Column(name = "thread_id", nullable = false)
    private Long threadId;

    @Column(name = "sender_id", nullable = false, length = 20)
    private String senderId;

    @Column(nullable = false, columnDefinition = "text")
    private String message;

    @Enumerated(STRING)
    @Column(name = "related_type", nullable = false, length = 30)
    private TeamMessageRelatedType relatedType;

    @Column(name = "related_id")
    private Long relatedId;

    @Column(name = "is_important", nullable = false)
    private boolean important;

    public TeamMessage toDomain() {
        return TeamMessage.builder()
            .id(this.id)
            .threadId(this.threadId)
            .senderId(this.senderId)
            .message(this.message)
            .relatedType(this.relatedType)
            .relatedId(this.relatedId)
            .important(this.important)
            .createdAt(this.getCreatedAt())
            .build();
    }

    public static TeamMessageJpaEntity toEntity(TeamMessage teamMessage) {
        return TeamMessageJpaEntity.builder()
            .id(teamMessage.getId())
            .threadId(teamMessage.getThreadId())
            .senderId(teamMessage.getSenderId())
            .message(teamMessage.getMessage())
            .relatedType(teamMessage.getRelatedType())
            .relatedId(teamMessage.getRelatedId())
            .important(teamMessage.isImportant())
            .build();
    }
}
