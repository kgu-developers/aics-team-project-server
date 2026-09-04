package project.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.fasterxml.jackson.databind.ObjectMapper;
import kgu.developers.api.project.application.ProjectFacade;
import kgu.developers.api.project.presentation.request.ProjectRequest;
import kgu.developers.api.team.application.TeamAccessValidator;
import kgu.developers.domain.project.application.command.ProjectCommandService;
import kgu.developers.domain.project.application.query.ProjectQueryService;
import kgu.developers.domain.project.domain.ApprovalStatus;
import kgu.developers.domain.project.domain.Project;
import kgu.developers.domain.projectApproval.domain.ApprovalCount;
import kgu.developers.domain.projectApproval.domain.ProjectApprovalRepository;
import kgu.developers.domain.projectApproval.application.command.ProjectApprovalCommandService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class ProjectFacadeTest {

    private static final Long TEAM_ID = 1L;
    private static final String MEMBER_ID = "202412345";

    @Mock private ProjectCommandService projectCommandService;
    @Mock private ProjectQueryService projectQueryService;
    @Mock private TeamAccessValidator teamAccessValidator;
    @Mock private ProjectApprovalRepository projectApprovalRepository;
    @Mock private ProjectApprovalCommandService projectApprovalCommandService;
    @InjectMocks private ProjectFacade projectFacade;

    @Test
    @DisplayName("getProject는 팀원에게 프로젝트 제안서를 반환한다")
    void getProject() {
        org.mockito.BDDMockito.willDoNothing().given(teamAccessValidator).validateMembership(TEAM_ID, MEMBER_ID);
        given(projectQueryService.getProjectByTeamId(TEAM_ID)).willReturn(project());

        assertThat(projectFacade.getProject(TEAM_ID, MEMBER_ID).title()).isEqualTo("AI 학습 도우미");
    }

    @Test
    @DisplayName("saveProject는 팀원이 아니면 접근을 거부한다")
    void saveProject_deniesNonMember() throws Exception {
        org.mockito.BDDMockito.willThrow(new AccessDeniedException("접근 거부"))
            .given(teamAccessValidator).validateMembership(TEAM_ID, MEMBER_ID);

        assertThatThrownBy(() -> projectFacade.saveProject(TEAM_ID, MEMBER_ID, request()))
            .isInstanceOf(AccessDeniedException.class);
        then(projectCommandService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("saveProject는 요청 필드를 커맨드 서비스에 전달한다")
    void saveProject() throws Exception {
        org.mockito.BDDMockito.willDoNothing().given(teamAccessValidator).validateMembership(TEAM_ID, MEMBER_ID);
        given(projectCommandService.saveProject(org.mockito.ArgumentMatchers.eq(TEAM_ID), org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any())).willReturn(project());

        assertThat(projectFacade.saveProject(TEAM_ID, MEMBER_ID, request()).goal()).isEqualTo("피드백 자동화");
    }

    @Test
    @DisplayName("completeProposal은 팀장이고 모든 팀원이 승인하면 완료 처리한다")
    void completeProposal() {
        given(projectQueryService.getProject(10L)).willReturn(project());
        org.mockito.BDDMockito.willDoNothing().given(teamAccessValidator).validateTeamLeader(TEAM_ID, MEMBER_ID);

        projectFacade.completeProposal(10L, MEMBER_ID);

        then(projectCommandService).should().completeProposal(10L);
    }

    @Test
    @DisplayName("completeProposal은 팀 행을 잠근 뒤 팀장 권한과 동의 목록을 확인한다")
    void completeProposal_locksTeamBeforeValidation() {
        given(projectQueryService.getProject(10L)).willReturn(project());
        org.mockito.BDDMockito.willDoNothing().given(teamAccessValidator).validateTeamLeader(TEAM_ID, MEMBER_ID);

        projectFacade.completeProposal(10L, MEMBER_ID);

        InOrder inOrder = org.mockito.Mockito.inOrder(projectCommandService, teamAccessValidator);
        inOrder.verify(projectCommandService).lockTeam(TEAM_ID);
        inOrder.verify(teamAccessValidator).validateTeamLeader(TEAM_ID, MEMBER_ID);
        inOrder.verify(projectCommandService).completeProposal(10L);
    }

    @Test
    @DisplayName("completeProposal은 팀장이 아니면 접근을 거부한다")
    void completeProposal_deniesNonLeader() {
        given(projectQueryService.getProject(10L)).willReturn(project());
        org.mockito.BDDMockito.willThrow(new AccessDeniedException("접근 거부"))
            .given(teamAccessValidator).validateTeamLeader(TEAM_ID, MEMBER_ID);

        assertThatThrownBy(() -> projectFacade.completeProposal(10L, MEMBER_ID))
            .isInstanceOf(AccessDeniedException.class);
        then(projectCommandService).should(org.mockito.Mockito.never()).completeProposal(10L);
    }

    @Test
    @DisplayName("completeProposal은 동의 검증을 잠금 경계의 커맨드 서비스에 위임한다")
    void completeProposal_delegatesApprovalValidationToCommandService() {
        given(projectQueryService.getProject(10L)).willReturn(project());
        org.mockito.BDDMockito.willDoNothing().given(teamAccessValidator).validateTeamLeader(TEAM_ID, MEMBER_ID);
        projectFacade.completeProposal(10L, MEMBER_ID);

        then(projectCommandService).should().completeProposal(10L);
    }

    @Test
    @DisplayName("deleteProject는 팀장이면 제안서를 삭제한다")
    void deleteProject() {
        given(projectQueryService.getProject(10L)).willReturn(project());
        org.mockito.BDDMockito.willDoNothing().given(teamAccessValidator).validateTeamLeader(TEAM_ID, MEMBER_ID);

        projectFacade.deleteProject(10L, MEMBER_ID);

        then(projectCommandService).should().deleteProject(10L);
    }

    @Test
    @DisplayName("deleteProject는 팀장이 아니면 접근을 거부한다")
    void deleteProject_deniesNonLeader() {
        given(projectQueryService.getProject(10L)).willReturn(project());
        org.mockito.BDDMockito.willThrow(new AccessDeniedException("접근 거부"))
            .given(teamAccessValidator).validateTeamLeader(TEAM_ID, MEMBER_ID);

        assertThatThrownBy(() -> projectFacade.deleteProject(10L, MEMBER_ID))
            .isInstanceOf(AccessDeniedException.class);
        then(projectCommandService).should(org.mockito.Mockito.never()).deleteProject(10L);
    }

    @Test
    @DisplayName("approveProject는 본인 팀원 동의를 저장한다")
    void approveProject() {
        given(projectQueryService.getProject(10L)).willReturn(project());
        org.mockito.BDDMockito.willDoNothing().given(teamAccessValidator).validateMembership(TEAM_ID, MEMBER_ID);

        projectFacade.approveProject(10L, MEMBER_ID);

        then(projectApprovalCommandService).should().approve(10L, MEMBER_ID);
    }

    @Test
    @DisplayName("getApprovalSummary는 완료 인원과 전체 인원을 반환한다")
    void getApprovalSummary() {
        given(projectQueryService.getProject(10L)).willReturn(project());
        org.mockito.BDDMockito.willDoNothing().given(teamAccessValidator).validateMembership(TEAM_ID, MEMBER_ID);
        given(projectApprovalRepository.countApprovalsByTeamMembers(10L, TEAM_ID, 0L))
            .willReturn(new ApprovalCount(2L, 1L));

        var response = projectFacade.getApprovalSummary(10L, MEMBER_ID);

        assertThat(response.approvedCount()).isEqualTo(1);
        assertThat(response.totalCount()).isEqualTo(2);
        assertThat(response.progress()).isEqualTo("1/2");
    }

    private ProjectRequest request() throws Exception {
        return new ProjectRequest("AI 학습 도우미", "설명", "피드백 자동화", "대면", "https://github.com/kgu/project",
            new ObjectMapper().readTree("[]"));
    }

    private Project project() {
        return Project.builder().id(10L).teamId(TEAM_ID).title("AI 학습 도우미").description("설명")
            .goal("피드백 자동화").approvalStatus(ApprovalStatus.DRAFT).build();
    }
}
