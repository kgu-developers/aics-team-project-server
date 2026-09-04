package kgu.developers.domain.topicCandidate.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TopicCandidateTest {

    @Test
    @DisplayName("create는 전달받은 값으로 주제 후보를 생성한다")
    void create() {
        TopicCandidate topicCandidate = TopicCandidate.create(100L, "20230001", "AI 기반 스마트 홈 시스템", "IoT 센서와 머신러닝을 활용한 스마트 홈 자동화 시스템");

        assertThat(topicCandidate.getTeamId()).isEqualTo(100L);
        assertThat(topicCandidate.getProposerUserId()).isEqualTo("20230001");
        assertThat(topicCandidate.getTitle()).isEqualTo("AI 기반 스마트 홈 시스템");
        assertThat(topicCandidate.getDescription()).isEqualTo("IoT 센서와 머신러닝을 활용한 스마트 홈 자동화 시스템");
        assertThat(topicCandidate.getDeletedAt()).isNull();
    }

    @Test
    @DisplayName("create는 null인 teamId가 전달되면 예외를 발생시킨다")
    void createWithNullTeamId() {
        assertThatThrownBy(() -> TopicCandidate.create(null, "20230001", "AI 기반 스마트 홈 시스템", "설명"))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("teamId");
    }

    @Test
    @DisplayName("create는 null인 proposerUserId가 전달되면 예외를 발생시킨다")
    void createWithNullProposerUserId() {
        assertThatThrownBy(() -> TopicCandidate.create(100L, null, "AI 기반 스마트 홈 시스템", "설명"))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("proposerUserId");
    }

    @Test
    @DisplayName("create는 null인 title이 전달되면 예외를 발생시킨다")
    void createWithNullTitle() {
        assertThatThrownBy(() -> TopicCandidate.create(100L, "20230001", null, "설명"))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("title");
    }

    @Test
    @DisplayName("create는 null인 description이 전달되면 예외를 발생시킨다")
    void createWithNullDescription() {
        assertThatThrownBy(() -> TopicCandidate.create(100L, "20230001", "AI 기반 스마트 홈 시스템", null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("description");
    }

    @Test
    @DisplayName("update 메서드들은 각 필드를 변경한다")
    void update() {
        TopicCandidate topicCandidate = TopicCandidate.create(100L, "20230001", "AI 기반 스마트 홈 시스템", "IoT 센서와 머신러닝을 활용한 스마트 홈 자동화 시스템");

        topicCandidate.updateTitle("블록체인 기반 투표 시스템");
        topicCandidate.updateDescription("탈중앙화된 투표 시스템 구현");

        assertThat(topicCandidate.getTeamId()).isEqualTo(100L);
        assertThat(topicCandidate.getTitle()).isEqualTo("블록체인 기반 투표 시스템");
        assertThat(topicCandidate.getDescription()).isEqualTo("탈중앙화된 투표 시스템 구현");
    }

    @Test
    @DisplayName("update 메서드들은 null이 전달되면 예외를 발생시킨다")
    void updateWithNull() {
        TopicCandidate topicCandidate = TopicCandidate.create(100L, "20230001", "AI 기반 스마트 홈 시스템", "IoT 센서와 머신러닝을 활용한 스마트 홈 자동화 시스템");

        assertThatThrownBy(() -> topicCandidate.updateTitle(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("title");
        assertThatThrownBy(() -> topicCandidate.updateDescription(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("description");
    }

    @Test
    @DisplayName("delete는 삭제 시각을 기록한다")
    void delete() {
        TopicCandidate topicCandidate = TopicCandidate.create(100L, "20230001", "AI 기반 스마트 홈 시스템", "IoT 센서와 머신러닝을 활용한 스마트 홈 자동화 시스템");

        topicCandidate.delete();

        assertThat(topicCandidate.getDeletedAt()).isNotNull();
    }
}
