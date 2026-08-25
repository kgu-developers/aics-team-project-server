package topiccandidate.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.List;
import kgu.developers.api.team.application.TeamAccessValidator;
import kgu.developers.api.topiccandidate.application.TopicCandidateFacade;
import kgu.developers.api.topiccandidate.presentation.response.TopicCandidateListResponse;
import kgu.developers.domain.topicCandidate.domain.TopicCandidate;
import kgu.developers.domain.topicCandidate.domain.TopicCandidateRepository;
import kgu.developers.domain.topicVote.domain.TopicVote;
import kgu.developers.domain.topicVote.domain.TopicVoteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TopicCandidateFacadeTest {

    private static final Long TEAM_ID = 1L;
    private static final String CURRENT_USER_ID = "202412345";

    private TopicCandidateRepository topicCandidateRepository;
    private TopicVoteRepository topicVoteRepository;
    private TeamAccessValidator teamAccessValidator;
    private TopicCandidateFacade topicCandidateFacade;

    @BeforeEach
    void setUp() {
        topicCandidateRepository = mock(TopicCandidateRepository.class);
        topicVoteRepository = mock(TopicVoteRepository.class);
        teamAccessValidator = mock(TeamAccessValidator.class);
        topicCandidateFacade = new TopicCandidateFacade(
            topicCandidateRepository,
            topicVoteRepository,
            teamAccessValidator
        );
    }

    @Test
    @DisplayName("getTopicCandidates는 후보별 득표 수와 현재 사용자의 투표 여부를 반환한다")
    void getTopicCandidates_ReturnsVoteCountAndVotedByMe() {
        // given
        TopicCandidate firstCandidate = candidate(1L, "AI 기반 학습 도우미", "첫 번째 설명");
        TopicCandidate secondCandidate = candidate(2L, "팀 프로젝트 관리", "두 번째 설명");
        given(topicCandidateRepository.findByTeamId(TEAM_ID)).willReturn(List.of(firstCandidate, secondCandidate));
        given(topicVoteRepository.findAllByCandidateIdIn(List.of(1L, 2L))).willReturn(List.of(
            vote(1L, CURRENT_USER_ID),
            vote(1L, "202412346"),
            vote(2L, "202412346")
        ));

        // when
        TopicCandidateListResponse result = topicCandidateFacade.getTopicCandidates(TEAM_ID, CURRENT_USER_ID);

        // then
        assertThat(result.contents()).hasSize(2);
        assertThat(result.contents().get(0))
            .extracting(
                TopicCandidateListResponse.TopicCandidateResponse::id,
                TopicCandidateListResponse.TopicCandidateResponse::proposerUserId,
                TopicCandidateListResponse.TopicCandidateResponse::description,
                TopicCandidateListResponse.TopicCandidateResponse::voteCount,
                TopicCandidateListResponse.TopicCandidateResponse::votedByMe
            )
            .containsExactly(1L, CURRENT_USER_ID, "첫 번째 설명", 2L, true);
        assertThat(result.contents().get(1))
            .extracting(
                TopicCandidateListResponse.TopicCandidateResponse::id,
                TopicCandidateListResponse.TopicCandidateResponse::voteCount,
                TopicCandidateListResponse.TopicCandidateResponse::votedByMe
            )
            .containsExactly(2L, 1L, false);
        verify(teamAccessValidator).validateMembership(TEAM_ID, CURRENT_USER_ID);
    }

    @Test
    @DisplayName("getTopicCandidates는 후보가 없으면 빈 목록을 반환한다")
    void getTopicCandidates_ReturnsEmptyList_WhenNoCandidatesExist() {
        // given
        given(topicCandidateRepository.findByTeamId(TEAM_ID)).willReturn(List.of());
        given(topicVoteRepository.findAllByCandidateIdIn(List.of())).willReturn(List.of());

        // when
        TopicCandidateListResponse result = topicCandidateFacade.getTopicCandidates(TEAM_ID, CURRENT_USER_ID);

        // then
        assertThat(result.contents()).isEmpty();
        verify(teamAccessValidator).validateMembership(TEAM_ID, CURRENT_USER_ID);
    }

    private TopicCandidate candidate(Long id, String title, String description) {
        return TopicCandidate.builder()
            .id(id)
            .teamId(TEAM_ID)
            .proposerUserId(CURRENT_USER_ID)
            .title(title)
            .description(description)
            .build();
    }

    private TopicVote vote(Long candidateId, String voterUserId) {
        return TopicVote.builder()
            .candidateId(candidateId)
            .voterUserId(voterUserId)
            .build();
    }
}
