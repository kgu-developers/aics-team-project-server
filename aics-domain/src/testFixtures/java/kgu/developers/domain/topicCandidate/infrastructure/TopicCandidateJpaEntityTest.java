package kgu.developers.domain.topicCandidate.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import kgu.developers.domain.topicCandidate.domain.TopicCandidate;

class TopicCandidateJpaEntityTest {

    private TopicCandidate topicCandidate(LocalDateTime createdAt, LocalDateTime deletedAt) {
        return TopicCandidate.builder()
                .id(1L)
                .teamId(100L)
                .proposerUserId("20230001")
                .title("AI 기반 스마트 홈 시스템")
                .description("IoT 센서와 머신러닝을 활용한 스마트 홈 자동화 시스템")
                .createdAt(createdAt)
                .deletedAt(deletedAt)
                .build();
    }

    @Test
    @DisplayName("toEntity는 기존 주제 후보의 생성일과 삭제일을 그대로 옮긴다")
    void toEntityKeepsTimestamps() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 1, 1, 9, 0);
        LocalDateTime deletedAt = LocalDateTime.of(2026, 3, 1, 9, 0);

        TopicCandidateJpaEntity entity = TopicCandidateJpaEntity.toEntity(topicCandidate(createdAt, deletedAt));

        assertThat(entity.getCreatedAt()).isEqualTo(createdAt);
        assertThat(entity.getDeletedAt()).isEqualTo(deletedAt);
    }

    @Test
    @DisplayName("toEntity - toDomain 변환은 모든 필드를 보존한다")
    void roundTrip() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 1, 1, 9, 0);
        TopicCandidate origin = topicCandidate(createdAt, null);

        TopicCandidate restored = TopicCandidateJpaEntity.toEntity(origin).toDomain();

        assertThat(restored.getId()).isEqualTo(origin.getId());
        assertThat(restored.getTeamId()).isEqualTo(origin.getTeamId());
        assertThat(restored.getProposerUserId()).isEqualTo(origin.getProposerUserId());
        assertThat(restored.getTitle()).isEqualTo(origin.getTitle());
        assertThat(restored.getDescription()).isEqualTo(origin.getDescription());
        assertThat(restored.getCreatedAt()).isEqualTo(createdAt);
        assertThat(restored.getDeletedAt()).isNull();
    }
}
