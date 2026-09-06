package topicVote.application.command;

import kgu.developers.domain.topicCandidate.domain.TopicCandidate;
import kgu.developers.domain.topicCandidate.domain.TopicCandidateRepository;
import kgu.developers.domain.topicCandidate.exception.TopicCandidateNotFoundException;
import kgu.developers.domain.topicVote.application.command.TopicVoteCommandService;
import kgu.developers.domain.topicVote.domain.TopicVote;
import kgu.developers.domain.topicVote.domain.TopicVoteRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TopicVoteCommandServiceTest {

    private static final Long TEAM = 1L;
    private static final Long OTHER_TEAM = 2L;
    private static final Long CANDIDATE = 10L;
    private static final String VOTER = "20230001";

    @Mock
    private TopicVoteRepository topicVoteRepository;

    @Mock
    private TopicCandidateRepository topicCandidateRepository;

    @InjectMocks
    private TopicVoteCommandService topicVoteCommandService;

    private TopicCandidate candidateOf(Long teamId) {
        return TopicCandidate.create(teamId, "20230002", "제목", "설명");
    }

    @Test
    @DisplayName("후보가 해당 팀 소속이면 투표가 저장된다")
    void voteForCandidateInSameTeam() {
        TopicVote saved = TopicVote.create(TEAM, CANDIDATE, VOTER);
        given(topicCandidateRepository.findById(CANDIDATE)).willReturn(Optional.of(candidateOf(TEAM)));
        given(topicVoteRepository.upsert(any())).willReturn(saved);

        assertThat(topicVoteCommandService.vote(TEAM, CANDIDATE, VOTER)).isSameAs(saved);
    }

    @Test
    @DisplayName("후보가 없거나 삭제됐으면 TopicCandidateNotFoundException 이고 투표는 저장되지 않는다")
    void voteForMissingCandidate() {
        // findById 는 활성 후보만 주므로 삭제된 후보도 이 경로로 들어온다
        given(topicCandidateRepository.findById(CANDIDATE)).willReturn(Optional.empty());

        assertThatThrownBy(() -> topicVoteCommandService.vote(TEAM, CANDIDATE, VOTER))
                .isInstanceOf(TopicCandidateNotFoundException.class);
        verify(topicVoteRepository, never()).upsert(any());
    }

    @Test
    @DisplayName("다른 팀 후보에는 투표할 수 없다")
    void voteForCandidateInAnotherTeam() {
        given(topicCandidateRepository.findById(CANDIDATE)).willReturn(Optional.of(candidateOf(OTHER_TEAM)));

        assertThatThrownBy(() -> topicVoteCommandService.vote(TEAM, CANDIDATE, VOTER))
                .isInstanceOf(TopicCandidateNotFoundException.class);
        verify(topicVoteRepository, never()).upsert(any());
    }
}
