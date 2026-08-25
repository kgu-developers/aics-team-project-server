package topicvote.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.Optional;
import kgu.developers.api.team.application.TeamAccessValidator;
import kgu.developers.api.topicvote.application.TopicVoteFacade;
import kgu.developers.api.topicvote.presentation.response.TopicVotePersistResponse;
import kgu.developers.domain.topicCandidate.domain.TopicCandidate;
import kgu.developers.domain.topicCandidate.domain.TopicCandidateRepository;
import kgu.developers.domain.topicVote.application.command.TopicVoteCommandService;
import kgu.developers.domain.topicVote.domain.TopicVote;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TopicVoteFacadeTest {

    private static final Long TEAM_ID = 1L;
    private static final Long CANDIDATE_ID = 1L;
    private static final String VOTER_USER_ID = "202412345";

    private TopicCandidateRepository topicCandidateRepository;
    private TopicVoteCommandService topicVoteCommandService;
    private TeamAccessValidator teamAccessValidator;
    private TopicVoteFacade topicVoteFacade;

    @BeforeEach
    void setUp() {
        topicCandidateRepository = mock(TopicCandidateRepository.class);
        topicVoteCommandService = mock(TopicVoteCommandService.class);
        teamAccessValidator = mock(TeamAccessValidator.class);
        topicVoteFacade = new TopicVoteFacade(topicCandidateRepository, topicVoteCommandService, teamAccessValidator);
    }

    @Test
    @DisplayName("vote는 후보의 팀 소속을 검증한 뒤 투표를 등록한다")
    void vote_ValidatesMembershipAndCreatesVote() {
        // given
        given(topicCandidateRepository.findById(CANDIDATE_ID)).willReturn(Optional.of(
            TopicCandidate.builder().id(CANDIDATE_ID).teamId(TEAM_ID).build()
        ));
        given(topicVoteCommandService.vote(TEAM_ID, CANDIDATE_ID, VOTER_USER_ID)).willReturn(
            TopicVote.builder().id(1L).teamId(TEAM_ID).candidateId(CANDIDATE_ID).voterUserId(VOTER_USER_ID).build()
        );

        // when
        TopicVotePersistResponse result = topicVoteFacade.vote(CANDIDATE_ID, VOTER_USER_ID);

        // then
        assertThat(result)
            .extracting(TopicVotePersistResponse::id, TopicVotePersistResponse::candidateId, TopicVotePersistResponse::voterUserId)
            .containsExactly(1L, CANDIDATE_ID, VOTER_USER_ID);
        verify(teamAccessValidator).validateMembership(TEAM_ID, VOTER_USER_ID);
        verify(topicVoteCommandService).vote(TEAM_ID, CANDIDATE_ID, VOTER_USER_ID);
    }
}
