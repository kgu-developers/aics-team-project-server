package topiccandidate.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.List;
import kgu.developers.api.team.application.TeamAccessValidator;
import kgu.developers.api.topiccandidate.application.TopicCandidateFacade;
import kgu.developers.api.topiccandidate.presentation.request.TopicCandidateCreateRequest;
import kgu.developers.api.topiccandidate.presentation.response.TopicCandidateListResponse;
import kgu.developers.api.topiccandidate.presentation.response.TopicCandidatePersistResponse;
import kgu.developers.domain.topicCandidate.application.command.TopicCandidateCommandService;
import kgu.developers.domain.topicCandidate.domain.TopicCandidate;
import kgu.developers.domain.topicCandidate.domain.TopicCandidateRepository;
import kgu.developers.domain.topicVote.domain.TopicVote;
import kgu.developers.domain.topicVote.domain.TopicVoteRepository;
import kgu.developers.domain.project.domain.Project;
import kgu.developers.domain.project.domain.ProjectRepository;
import kgu.developers.api.topiccandidate.presentation.request.TopicFinalizeRequest;
import kgu.developers.api.topiccandidate.presentation.response.TopicFinalizeResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TopicCandidateFacadeTest {

    private static final Long TEAM_ID = 1L;
    private static final String CURRENT_USER_ID = "202412345";

    private TopicCandidateRepository topicCandidateRepository;
    private TopicVoteRepository topicVoteRepository;
    private ProjectRepository projectRepository;
    private TopicCandidateCommandService topicCandidateCommandService;
    private TeamAccessValidator teamAccessValidator;
    private TopicCandidateFacade topicCandidateFacade;

    @BeforeEach
    void setUp() {
        topicCandidateRepository = mock(TopicCandidateRepository.class);
        topicVoteRepository = mock(TopicVoteRepository.class);
        projectRepository = mock(ProjectRepository.class);
        topicCandidateCommandService = mock(TopicCandidateCommandService.class);
        teamAccessValidator = mock(TeamAccessValidator.class);
        topicCandidateFacade = new TopicCandidateFacade(
            topicCandidateCommandService,
            topicCandidateRepository,
            topicVoteRepository,
            projectRepository,
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

    @Test
    @DisplayName("createTopicCandidate는 인증된 팀원의 제목과 설명으로 후보를 등록한다")
    void createTopicCandidate_CreatesCandidate() {
        // given
        TopicCandidateCreateRequest request = new TopicCandidateCreateRequest("AI 기반 학습 도우미", "학생별 맞춤형 학습 계획을 지원합니다.");
        TopicCandidate topicCandidate = candidate(1L, request.title(), request.description());
        given(topicCandidateCommandService.createTopicCandidate(
            TEAM_ID, CURRENT_USER_ID, request.title(), request.description()
        )).willReturn(topicCandidate);

        // when
        TopicCandidatePersistResponse result = topicCandidateFacade.createTopicCandidate(TEAM_ID, CURRENT_USER_ID, request);

        // then
        assertThat(result)
            .extracting(
                TopicCandidatePersistResponse::id,
                TopicCandidatePersistResponse::proposerUserId,
                TopicCandidatePersistResponse::title,
                TopicCandidatePersistResponse::description
            )
            .containsExactly(1L, CURRENT_USER_ID, request.title(), request.description());
        verify(teamAccessValidator).validateMembership(TEAM_ID, CURRENT_USER_ID);
    }

    @Test
    @DisplayName("finalizeTopic은 팀장이 선택한 후보 제목으로 프로젝트를 생성한다")
    void finalizeTopic_CreatesProjectWithCandidateTitle() {
        // given
        TopicCandidate topicCandidate = candidate(1L, "AI 기반 학습 도우미", "학생별 맞춤형 학습 계획을 지원합니다.");
        Project savedProject = Project.builder()
            .id(2L)
            .teamId(TEAM_ID)
            .title(topicCandidate.getTitle())
            .description(topicCandidate.getDescription())
            .goal(topicCandidate.getDescription())
            .build();
        given(topicCandidateRepository.findById(topicCandidate.getId())).willReturn(java.util.Optional.of(topicCandidate));
        given(projectRepository.findAllByTeamId(TEAM_ID)).willReturn(List.of());
        given(projectRepository.save(org.mockito.ArgumentMatchers.any(Project.class))).willReturn(savedProject);

        // when
        TopicFinalizeResponse result = topicCandidateFacade.finalizeTopic(
            TEAM_ID, CURRENT_USER_ID, new TopicFinalizeRequest(topicCandidate.getId())
        );

        // then
        assertThat(result)
            .extracting(TopicFinalizeResponse::projectId, TopicFinalizeResponse::candidateId, TopicFinalizeResponse::title)
            .containsExactly(2L, topicCandidate.getId(), topicCandidate.getTitle());
        verify(teamAccessValidator).validateLeaderWithTeamLock(TEAM_ID, CURRENT_USER_ID);
        verify(projectRepository).save(org.mockito.ArgumentMatchers.argThat(project ->
            project.getTeamId().equals(TEAM_ID)
                && project.getTitle().equals(topicCandidate.getTitle())
                && project.getTopicCandidateId().equals(topicCandidate.getId())
                && project.getDescription().equals(topicCandidate.getDescription())
                && project.getGoal().equals(topicCandidate.getDescription())
        ));
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
