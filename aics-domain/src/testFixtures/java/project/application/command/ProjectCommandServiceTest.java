package project.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.fasterxml.jackson.databind.ObjectMapper;
import kgu.developers.common.exception.CustomException;
import kgu.developers.domain.project.application.command.ProjectCommandService;
import kgu.developers.domain.project.domain.ApprovalStatus;
import kgu.developers.domain.project.domain.Project;
import kgu.developers.domain.project.domain.ProjectRepository;
import kgu.developers.domain.projectApproval.domain.ProjectApproval;
import kgu.developers.domain.projectApproval.domain.ProjectApprovalRepository;
import kgu.developers.domain.teamMember.domain.TeamMember;
import kgu.developers.domain.teamMember.domain.TeamMemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class ProjectCommandServiceTest {

    @Mock private ProjectRepository projectRepository;
    @Mock private ProjectApprovalRepository projectApprovalRepository;
    @Mock private TeamMemberRepository teamMemberRepository;
    @InjectMocks private ProjectCommandService projectCommandService;

    @Test
    @DisplayName("saveProject는 기존 제안서가 없으면 DRAFT 상태로 생성한다")
    void saveProject_createsProject() throws Exception {
        given(projectRepository.findAllByTeamIdIncludingDeletedForUpdate(1L)).willReturn(List.of());
        given(projectRepository.save(org.mockito.ArgumentMatchers.any())).willAnswer(invocation -> invocation.getArgument(0));

        Project result = saveProject();

        assertThat(result.getApprovalStatus()).isEqualTo(ApprovalStatus.DRAFT);
        assertThat(result.getTitle()).isEqualTo("새 제목");
    }

    @Test
    @DisplayName("saveProject는 기존 제안서를 수정하면서 승인 이력을 삭제한다")
    void saveProject_updatesAndClearsApprovals() throws Exception {
        Project existing = Project.builder().id(10L).teamId(1L).title("기존 제목").description("기존 설명")
            .goal("기존 목표").approvalStatus(ApprovalStatus.APPROVED).build();
        given(projectRepository.findAllByTeamIdIncludingDeletedForUpdate(1L)).willReturn(List.of(existing));
        given(projectApprovalRepository.findAllByProjectId(10L)).willReturn(List.of(
            ProjectApproval.builder().id(100L).projectId(10L).userId("202412345").approvedAt(LocalDateTime.now()).build()
        ));
        given(projectRepository.save(existing)).willReturn(existing);

        Project result = saveProject();

        assertThat(result.getTitle()).isEqualTo("새 제목");
        assertThat(result.getApprovalStatus()).isEqualTo(ApprovalStatus.DRAFT);
        assertThat(result.getProposalRevision()).isEqualTo(1L);
        then(projectApprovalRepository).should().deleteAllByProjectId(10L);
    }

    @Test
    @DisplayName("saveProject는 내용이 같으면 승인 이력을 초기화하지 않는다")
    void saveProject_keepsApprovalsWhenContentIsUnchanged() throws Exception {
        Project existing = Project.builder().id(10L).teamId(1L).title("새 제목").description("새 설명")
            .goal("새 목표").meetingStyle("대면").repositoryUrl("https://github.com/kgu/project")
            .externalLinks(new ObjectMapper().readTree("[]")).approvalStatus(ApprovalStatus.DRAFT).build();
        given(projectRepository.findAllByTeamIdIncludingDeletedForUpdate(1L)).willReturn(List.of(existing));

        saveProject();

        then(projectApprovalRepository).shouldHaveNoInteractions();
        then(projectRepository).should(org.mockito.Mockito.never()).save(existing);
    }

    @Test
    @DisplayName("saveProject는 완료된 제안서를 수정할 수 없다")
    void saveProject_rejectsCompletedProject() throws Exception {
        Project completed = Project.builder().id(10L).teamId(1L).title("제목").description("설명").goal("목표")
            .approvalStatus(ApprovalStatus.DRAFT).proposalCompletedAt(LocalDateTime.now()).build();
        given(projectRepository.findAllByTeamIdIncludingDeletedForUpdate(1L)).willReturn(List.of(completed));

        assertThatThrownBy(this::saveProject).isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("completeProposal은 프로젝트의 제안 완료 시각을 설정하고 승인 상태를 APPROVED로 변경한다")
    void completeProposal() {
        Project project = Project.builder().id(10L).teamId(1L).title("제목").description("설명").goal("목표")
            .approvalStatus(ApprovalStatus.DRAFT).build();
        given(projectRepository.findByIdForUpdate(10L)).willReturn(Optional.of(project));
        given(teamMemberRepository.findAllByTeamId(1L)).willReturn(List.of(TeamMember.create(1L, "202412345", false, "개발자")));
        given(projectApprovalRepository.existsByProjectIdAndUserIdAndProposalRevision(10L, "202412345", project.getProposalRevision())).willReturn(true);

        projectCommandService.completeProposal(10L);

        assertThat(project.getProposalCompletedAt()).isNotNull();
        assertThat(project.getApprovalStatus()).isEqualTo(ApprovalStatus.APPROVED);
        then(projectRepository).should().save(project);
    }

    @Test
    @DisplayName("completeProposal은 팀원이 한 명도 없으면 예외를 던진다")
    void completeProposal_rejectsEmptyTeam() {
        Project project = Project.builder().id(10L).teamId(1L).title("제목").description("설명").goal("목표")
            .approvalStatus(ApprovalStatus.DRAFT).build();
        given(projectRepository.findByIdForUpdate(10L)).willReturn(Optional.of(project));
        given(teamMemberRepository.findAllByTeamId(1L)).willReturn(List.of());

        assertThatThrownBy(() -> projectCommandService.completeProposal(10L)).isInstanceOf(CustomException.class);
        then(projectRepository).should(org.mockito.Mockito.never()).save(project);
    }

    @Test
    @DisplayName("completeProposal은 이미 완료된 제안서면 예외를 던진다")
    void completeProposal_rejectsCompletedProject() {
        Project project = Project.builder().id(10L).teamId(1L).title("제목").description("설명").goal("목표")
            .approvalStatus(ApprovalStatus.DRAFT).proposalCompletedAt(LocalDateTime.now()).build();
        given(projectRepository.findByIdForUpdate(10L)).willReturn(Optional.of(project));

        assertThatThrownBy(() -> projectCommandService.completeProposal(10L)).isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("saveProject는 삭제된 프로젝트를 복원해 동일 팀의 새 제안서를 저장한다")
    void saveProject_restoresSoftDeletedProject() throws Exception {
        Project deleted = Project.builder().id(10L).teamId(1L).title("기존 제목").description("기존 설명")
            .goal("기존 목표").approvalStatus(ApprovalStatus.DRAFT).deletedAt(LocalDateTime.now()).build();
        given(projectRepository.findAllByTeamIdIncludingDeletedForUpdate(1L)).willReturn(List.of(deleted));
        given(projectRepository.save(deleted)).willReturn(deleted);

        Project result = saveProject();

        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getDeletedAt()).isNull();
        assertThat(result.getTitle()).isEqualTo("새 제목");
        assertThat(result.getProposalRevision()).isEqualTo(1L);
        then(projectApprovalRepository).should().deleteAllByProjectId(10L);
    }

    @Test
    @DisplayName("completeProposal은 현재 제안서 리비전에 대한 동의만 확인한다")
    void completeProposal_requiresApprovalForCurrentRevision() {
        Project project = Project.builder().id(10L).teamId(1L).title("제목").description("설명").goal("목표")
            .approvalStatus(ApprovalStatus.DRAFT).proposalRevision(2L).build();
        given(projectRepository.findByIdForUpdate(10L)).willReturn(Optional.of(project));
        given(teamMemberRepository.findAllByTeamId(1L)).willReturn(List.of(TeamMember.create(1L, "202412345", false, "개발자")));
        given(projectApprovalRepository.existsByProjectIdAndUserIdAndProposalRevision(10L, "202412345", 2L)).willReturn(false);

        assertThatThrownBy(() -> projectCommandService.completeProposal(10L)).isInstanceOf(CustomException.class);
        then(projectRepository).should(org.mockito.Mockito.never()).save(project);
    }

    @Test
    @DisplayName("saveProject는 조회 전에 팀 행을 잠가 최초 등록 경합을 막는다")
    void saveProject_locksTeamBeforeLookup() throws Exception {
        given(projectRepository.findAllByTeamIdIncludingDeletedForUpdate(1L)).willReturn(List.of());
        given(projectRepository.save(org.mockito.ArgumentMatchers.any())).willAnswer(invocation -> invocation.getArgument(0));

        saveProject();

        org.mockito.InOrder inOrder = org.mockito.Mockito.inOrder(projectRepository);
        inOrder.verify(projectRepository).lockTeam(1L);
        inOrder.verify(projectRepository).findAllByTeamIdIncludingDeletedForUpdate(1L);
    }

    private Project saveProject() throws Exception {
        return projectCommandService.saveProject(1L, "새 제목", "새 설명", "새 목표", "대면",
            "https://github.com/kgu/project", new ObjectMapper().readTree("[]"));
    }
}
