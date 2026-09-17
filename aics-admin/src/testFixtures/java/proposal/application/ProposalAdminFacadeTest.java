package proposal.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import kgu.developers.admin.proposal.application.ProposalAdminFacade;
import kgu.developers.admin.proposal.presentation.request.ProposalFeedbackAdminRequest;
import kgu.developers.admin.proposal.presentation.response.ProposalFeedbackAdminPageResponse;
import kgu.developers.admin.proposal.presentation.response.ProposalFeedbackAdminResponse;
import kgu.developers.domain.project.application.command.ProjectCommandService;
import kgu.developers.domain.project.application.query.ProjectQueryService;
import kgu.developers.domain.project.domain.Project;
import kgu.developers.domain.project.exception.ProjectNotFoundException;
import kgu.developers.domain.section.application.query.SectionQueryService;
import kgu.developers.domain.team.domain.Team;
import kgu.developers.domain.team.domain.TeamRepository;
import kgu.developers.domain.teammessage.application.command.TeamMessageCommandService;
import kgu.developers.domain.teammessage.application.query.TeamMessageQueryService;
import kgu.developers.domain.teammessage.domain.TeamMessage;
import kgu.developers.domain.teammessage.domain.TeamMessageRelatedType;
import kgu.developers.domain.teamthread.application.command.TeamThreadCommandService;
import kgu.developers.domain.teamthread.application.query.TeamThreadQueryService;
import kgu.developers.domain.teamthread.domain.TeamThread;
import kgu.developers.domain.user.application.query.UserQueryService;
import kgu.developers.domain.user.domain.User;
import kgu.developers.domain.user.domain.UserGlobalRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class ProposalAdminFacadeTest {

    private static final Long SECTION_ID = 1L;
    private static final Long TEAM_ID = 10L;
    private static final Long PROJECT_ID = 200L;
    private static final Long THREAD_ID = 50L;
    private static final String PROFESSOR_ID = "202699999";
    private static final String FEEDBACK = "데이터 구성의 수집 방법을 구체적으로 적어 주세요.";

    @Mock
    private SectionQueryService sectionQueryService;

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private ProjectQueryService projectQueryService;

    @Mock
    private ProjectCommandService projectCommandService;

    @Mock
    private TeamThreadCommandService teamThreadCommandService;

    @Mock
    private TeamThreadQueryService teamThreadQueryService;

    @Mock
    private TeamMessageCommandService teamMessageCommandService;

    @Mock
    private TeamMessageQueryService teamMessageQueryService;

    @Mock
    private UserQueryService userQueryService;

    @InjectMocks
    private ProposalAdminFacade proposalAdminFacade;

    @Test
    @DisplayName("제안서 피드백을 등록하면 TeamMessage로 발행되고 제안서가 수정 요청 상태로 리오픈된다")
    void postFeedback_Success() {
        given(sectionQueryService.isActiveSectionOwnedByProfessor(SECTION_ID, PROFESSOR_ID)).willReturn(true);
        given(teamRepository.findById(TEAM_ID)).willReturn(Optional.of(team(SECTION_ID)));
        given(projectQueryService.getProjectByTeamId(TEAM_ID)).willReturn(project());
        given(teamThreadCommandService.getOrCreateThread(TEAM_ID))
            .willReturn(TeamThread.builder().id(THREAD_ID).teamId(TEAM_ID).build());
        given(teamMessageCommandService.postMessage(
            THREAD_ID, PROFESSOR_ID, TeamMessageRelatedType.PROPOSAL, PROJECT_ID, FEEDBACK))
            .willReturn(message());
        given(userQueryService.getUserByStudentNumber(PROFESSOR_ID)).willReturn(
            User.create(PROFESSOR_ID, "prof@kyonggi.ac.kr", "김교수", "pw", UserGlobalRole.ADMIN, "010-1111-2222"));

        ProposalFeedbackAdminResponse response = proposalAdminFacade.postFeedback(
            SECTION_ID, TEAM_ID, new ProposalFeedbackAdminRequest(FEEDBACK), PROFESSOR_ID);

        assertThat(response.messageId()).isEqualTo(500L);
        assertThat(response.projectId()).isEqualTo(PROJECT_ID);
        assertThat(response.senderName()).isEqualTo("김교수");
        assertThat(response.message()).isEqualTo(FEEDBACK);
        assertThat(response.createdAt()).isEqualTo("2026-09-13 14:00");
        verify(projectCommandService).reopenProposal(PROJECT_ID);
    }

    @Test
    @DisplayName("담당 분반이 아니면 제안서 피드백을 등록할 수 없다")
    void postFeedback_OtherProfessor() {
        given(sectionQueryService.isActiveSectionOwnedByProfessor(SECTION_ID, PROFESSOR_ID)).willReturn(false);

        assertThatThrownBy(() -> proposalAdminFacade.postFeedback(
            SECTION_ID, TEAM_ID, new ProposalFeedbackAdminRequest(FEEDBACK), PROFESSOR_ID))
            .isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(projectCommandService, teamMessageCommandService);
    }

    @Test
    @DisplayName("다른 분반의 팀이면 제안서 피드백을 등록할 수 없다")
    void postFeedback_TeamOfAnotherSection() {
        given(sectionQueryService.isActiveSectionOwnedByProfessor(SECTION_ID, PROFESSOR_ID)).willReturn(true);
        given(teamRepository.findById(TEAM_ID)).willReturn(Optional.of(team(999L)));

        assertThatThrownBy(() -> proposalAdminFacade.postFeedback(
            SECTION_ID, TEAM_ID, new ProposalFeedbackAdminRequest(FEEDBACK), PROFESSOR_ID))
            .isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(projectCommandService, teamMessageCommandService);
    }

    @Test
    @DisplayName("피드백 이력은 PROPOSAL 타입 팀 메시지를 최신순으로 반환한다")
    void getFeedbacks_Success() {
        given(sectionQueryService.isActiveSectionOwnedByProfessor(SECTION_ID, PROFESSOR_ID)).willReturn(true);
        given(teamRepository.findById(TEAM_ID)).willReturn(Optional.of(team(SECTION_ID)));
        given(projectQueryService.getProjectByTeamId(TEAM_ID)).willReturn(project());
        given(teamThreadQueryService.getThread(TEAM_ID))
            .willReturn(TeamThread.builder().id(THREAD_ID).teamId(TEAM_ID).build());
        PageRequest pageable = PageRequest.of(0, 20);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        given(teamMessageQueryService.getMessages(
            eq(THREAD_ID),
            eq(TeamMessageRelatedType.PROPOSAL),
            eq(PROJECT_ID),
            pageableCaptor.capture()))
            .willReturn(new PageImpl<>(List.of(message(502L, "두 번째 피드백"), message(500L, FEEDBACK)), pageable, 2));
        given(userQueryService.getUsersByStudentNumbersIncludingDeleted(List.of(PROFESSOR_ID))).willReturn(List.of(
            User.create(PROFESSOR_ID, "prof@kyonggi.ac.kr", "김교수", "pw", UserGlobalRole.ADMIN, "010-1111-2222")));

        ProposalFeedbackAdminPageResponse response = proposalAdminFacade.getFeedbacks(
            SECTION_ID, TEAM_ID, pageable, PROFESSOR_ID);

        // 정렬은 조회 시점에 결정되므로, 전달한 Pageable에 id 내림차순이 실려 있어야 한다.
        assertThat(pageableCaptor.getValue().getSort()).isEqualTo(Sort.by(Sort.Order.desc("id")));
        assertThat(response.contents()).extracting(ProposalFeedbackAdminResponse::messageId)
            .containsExactly(502L, 500L);
        assertThat(response.contents().get(0).senderName()).isEqualTo("김교수");
        assertThat(response.pageable().totalElements()).isEqualTo(2L);
    }

    @Test
    @DisplayName("제안서가 아직 없으면 피드백 이력은 빈 목록이다")
    void getFeedbacks_NoProposal() {
        given(sectionQueryService.isActiveSectionOwnedByProfessor(SECTION_ID, PROFESSOR_ID)).willReturn(true);
        given(teamRepository.findById(TEAM_ID)).willReturn(Optional.of(team(SECTION_ID)));
        given(projectQueryService.getProjectByTeamId(TEAM_ID)).willThrow(new ProjectNotFoundException());

        ProposalFeedbackAdminPageResponse response = proposalAdminFacade.getFeedbacks(
            SECTION_ID, TEAM_ID, PageRequest.of(0, 20), PROFESSOR_ID);

        assertThat(response.contents()).isEmpty();
        assertThat(response.pageable().isEnd()).isTrue();
        verifyNoInteractions(teamMessageQueryService);
    }

    private Team team(Long sectionId) {
        return Team.builder().id(TEAM_ID).sectionId(sectionId).name("A팀").build();
    }

    private Project project() {
        return Project.builder().id(PROJECT_ID).teamId(TEAM_ID).title("A팀 제안서").build();
    }

    private TeamMessage message() {
        return message(500L, FEEDBACK);
    }

    private TeamMessage message(Long id, String message) {
        return TeamMessage.builder()
            .id(id)
            .threadId(THREAD_ID)
            .senderId(PROFESSOR_ID)
            .relatedType(TeamMessageRelatedType.PROPOSAL)
            .relatedId(PROJECT_ID)
            .message(message)
            .createdAt(LocalDateTime.of(2026, 9, 13, 14, 0))
            .build();
    }
}
