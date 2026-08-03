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

    // TeamThread는 같은 모듈 내 애그리거트이지만 연관관계 매핑 없이 id로만 참조한다.
    @Column(name = "thread_id", nullable = false)
    private Long threadId;

    // User 엔티티는 아직 구현되지 않았으므로 FK 매핑 없이 학번(student_number) 문자열로만 참조한다.
    @Column(name = "sender_id", nullable = false, length = 20)
    private String senderId;

    @Column(nullable = false, columnDefinition = "text")
    private String message;

    @Enumerated(STRING)
    @Column(name = "related_type", nullable = false, length = 30)
    private TeamMessageRelatedType relatedType;

    // 폴리모픽 참조(related_type에 따라 대상 테이블이 다름) — 의도적으로 FK 없음.
    @Column(name = "related_id")
    private Long relatedId;

    @Column(name = "is_important", nullable = false)
    private boolean important;

    @Column(name = "is_read", nullable = false)
    private boolean read;

    public TeamMessage toDomain() {
        return TeamMessage.builder()
            .id(this.id)
            .threadId(this.threadId)
            .senderId(this.senderId)
            .message(this.message)
            .relatedType(this.relatedType)
            .relatedId(this.relatedId)
            .important(this.important)
            .read(this.read)
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
            .read(teamMessage.isRead())
            .build();
    }
}
