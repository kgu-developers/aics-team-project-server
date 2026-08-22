package topicVote.infrastructure;

import kgu.developers.domain.topicVote.domain.TopicVote;
import kgu.developers.domain.topicVote.infrastructure.TopicVoteJpaEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class TopicVoteJpaEntityTest {

    @Test
    @DisplayName("TopicVote <-> TopicVoteJpaEntity 양방향 변환 시 필드가 보존된다")
    void roundTrip() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 1, 1, 9, 0);
        TopicVote origin = TopicVote.builder()
                .id(1L)
                .candidateId(10L)
                .voterUserId("20230001")
                .createdAt(createdAt)
                .build();

        TopicVoteJpaEntity entity = TopicVoteJpaEntity.toEntity(origin);
        TopicVote restored = entity.toDomain();

        assertThat(restored.getId()).isEqualTo(origin.getId());
        assertThat(restored.getCandidateId()).isEqualTo(origin.getCandidateId());
        assertThat(restored.getVoterUserId()).isEqualTo(origin.getVoterUserId());
        assertThat(restored.getCreatedAt()).isEqualTo(createdAt);
        assertThat(restored.getDeletedAt()).isNull();
    }

    @Test
    @DisplayName("toEntity 호출 시 삭제일(deletedAt) 시각이 올바르게 전달된다")
    void carriesDeletedAt() {
        LocalDateTime deletedAt = LocalDateTime.of(2026, 5, 1, 12, 0);
        TopicVote topicVote = TopicVote.builder()
                .id(1L)
                .candidateId(10L)
                .voterUserId("20230001")
                .deletedAt(deletedAt)
                .build();

        TopicVoteJpaEntity entity = TopicVoteJpaEntity.toEntity(topicVote);

        assertThat(entity.getDeletedAt()).isEqualTo(deletedAt);
    }
}
