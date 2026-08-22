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
    name = "\"topic_candidate\"",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"team_id", "title"}, name = "uk_team_title")
    },
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

    @Column(nullable = false)
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
        entity.setDeletedAt(topicCandidate.getDeletedAt());
        return entity;
    }
}
