package topicVote.domain;

import kgu.developers.domain.topicVote.domain.TopicVote;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TopicVoteTest {

    @Test
    @DisplayName("TopicVote.create 로 정적 팩토리 생성 시 올바르게 객체가 생성된다")
    void createTopicVote() {
        TopicVote topicVote = TopicVote.create(1L, "20230001");

        assertThat(topicVote.getCandidateId()).isEqualTo(1L);
        assertThat(topicVote.getVoterUserId()).isEqualTo("20230001");
    }

    @Test
    @DisplayName("candidateId가 null이면 NullPointerException이 발생한다")
    void createTopicVoteWithNullCandidateId() {
        assertThatThrownBy(() -> TopicVote.create(null, "20230001"))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("candidateId");
    }

    @Test
    @DisplayName("voterUserId가 null이면 NullPointerException이 발생한다")
    void createTopicVoteWithNullVoterUserId() {
        assertThatThrownBy(() -> TopicVote.create(1L, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("voterUserId");
    }

    @Test
    @DisplayName("candidateId를 수정할 수 있다")
    void updateCandidateId() {
        TopicVote topicVote = TopicVote.create(1L, "20230001");

        topicVote.updateCandidateId(2L);

        assertThat(topicVote.getCandidateId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("voterUserId를 수정할 수 있다")
    void updateVoterUserId() {
        TopicVote topicVote = TopicVote.create(1L, "20230001");

        topicVote.updateVoterUserId("20230002");

        assertThat(topicVote.getVoterUserId()).isEqualTo("20230002");
    }

    @Test
    @DisplayName("delete 호출 시 deletedAt 필드에 현재 시각이 기록된다")
    void deleteTopicVote() {
        TopicVote topicVote = TopicVote.create(1L, "20230001");

        assertThat(topicVote.getDeletedAt()).isNull();
        topicVote.delete();
        assertThat(topicVote.getDeletedAt()).isNotNull();
    }
}
