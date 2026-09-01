package kgu.developers.domain.topicVote.infrastructure;

import jakarta.persistence.*;
import kgu.developers.common.domain.BaseTimeEntity;
import kgu.developers.domain.topicVote.domain.TopicVote;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

@Entity
@Table(
		name = "topic_vote",
		uniqueConstraints = @UniqueConstraint(name = "uk_topic_vote_team_voter", columnNames = {"team_id", "voter_user_id"}),
		indexes = @Index(name = "idx_topic_vote_candidate", columnList = "candidate_id, deleted_at")
)
@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = PROTECTED)
public class TopicVoteJpaEntity extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @Column(name = "team_id", nullable = false)
    private Long teamId;

    @Column(name = "candidate_id", nullable = false)
    private Long candidateId;

    @Column(name = "voter_user_id", nullable = false, length = 20)
    private String voterUserId;

    public TopicVote toDomain() {
        return TopicVote.builder()
                .id(id)
                .teamId(teamId)
                .candidateId(candidateId)
                .voterUserId(voterUserId)
                .createdAt(getCreatedAt())
                .updatedAt(getUpdatedAt())
                .deletedAt(getDeletedAt())
                .build();
    }

    public static TopicVoteJpaEntity toEntity(TopicVote topicVote) {
        TopicVoteJpaEntity entity = TopicVoteJpaEntity.builder()
                .id(topicVote.getId())
                .teamId(topicVote.getTeamId())
                .candidateId(topicVote.getCandidateId())
                .voterUserId(topicVote.getVoterUserId())
                .build();
        entity.createdAt = topicVote.getCreatedAt();
        entity.setDeletedAt(topicVote.getDeletedAt());
        return entity;
    }
}
