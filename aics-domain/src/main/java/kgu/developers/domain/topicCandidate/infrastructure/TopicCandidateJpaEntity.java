package kgu.developers.domain.topicCandidate.infrastructure;

import jakarta.persistence.*;
import kgu.developers.common.domain.BaseTimeEntity;
import kgu.developers.domain.topicCandidate.domain.TopicCandidate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

@Entity
@Table(
    // 유니크 규칙은 엔티티로 표현할 수 없다. 소프트 삭제된 후보가 자리를 점유하면 재제출이 영구히 막히므로
    // deleted_at IS NULL 부분 유니크 인덱스로 걸며, DDL 은 database/topic_candidate.sql 에 있다.
    name = "\"topic_candidate\"",
    indexes = {
        @Index(columnList = "team_id", name = "idx_team_id"),
        @Index(columnList = "proposer_user_id", name = "idx_proposer_user_id")
    }
)
@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = PROTECTED)
public class TopicCandidateJpaEntity extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false)
    private Long teamId;

    @Column(nullable = false, length = 20)
    private String proposerUserId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    public TopicCandidate toDomain() {
        return TopicCandidate.builder()
                .id(id)
                .teamId(teamId)
                .proposerUserId(proposerUserId)
                .title(title)
                .description(description)
                .createdAt(getCreatedAt())
                .updatedAt(getUpdatedAt())
                .deletedAt(getDeletedAt())
                .build();
    }

    public static TopicCandidateJpaEntity toEntity(TopicCandidate topicCandidate) {
        TopicCandidateJpaEntity entity = TopicCandidateJpaEntity.builder()
                .id(topicCandidate.getId())
                .teamId(topicCandidate.getTeamId())
                .proposerUserId(topicCandidate.getProposerUserId())
                .title(topicCandidate.getTitle())
                .description(topicCandidate.getDescription())
                .build();
        entity.createdAt = topicCandidate.getCreatedAt();
        entity.updatedAt = topicCandidate.getUpdatedAt();
        entity.deletedAt = topicCandidate.getDeletedAt();
        return entity;
    }
}
