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
    @InjectMocks private ProjectCommandService projectCommandService;

    @Test
    @DisplayName("saveProject는 기존 제안서가 없으면 DRAFT 상태로 생성한다")
    void saveProject_createsProject() throws Exception {
        given(projectRepository.findAllByTeamId(1L)).willReturn(List.of());
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
        given(projectRepository.findAllByTeamId(1L)).willReturn(List.of(existing));
        given(projectApprovalRepository.findAllByProjectId(10L)).willReturn(List.of(
            ProjectApproval.builder().id(100L).projectId(10L).userId("202412345").approvedAt(LocalDateTime.now()).build()
        ));
        given(projectRepository.save(existing)).willReturn(existing);

        Project result = saveProject();

        assertThat(result.getTitle()).isEqualTo("새 제목");
        assertThat(result.getApprovalStatus()).isEqualTo(ApprovalStatus.DRAFT);
        then(projectApprovalRepository).should().deleteById(100L);
    }

    @Test
    @DisplayName("saveProject는 완료된 제안서를 수정할 수 없다")
    void saveProject_rejectsCompletedProject() throws Exception {
        Project completed = Project.builder().id(10L).teamId(1L).title("제목").description("설명").goal("목표")
            .approvalStatus(ApprovalStatus.DRAFT).proposalCompletedAt(LocalDateTime.now()).build();
        given(projectRepository.findAllByTeamId(1L)).willReturn(List.of(completed));

        assertThatThrownBy(this::saveProject).isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("completeProposal은 프로젝트의 제안 완료 시각을 설정한다")
    void completeProposal() {
        Project project = Project.builder().id(10L).teamId(1L).title("제목").description("설명").goal("목표")
            .approvalStatus(ApprovalStatus.DRAFT).build();
        given(projectRepository.findById(10L)).willReturn(Optional.of(project));

        projectCommandService.completeProposal(10L);

        assertThat(project.getProposalCompletedAt()).isNotNull();
        then(projectRepository).should().save(project);
    }

    private Project saveProject() throws Exception {
        return projectCommandService.saveProject(1L, "새 제목", "새 설명", "새 목표", "대면",
            "https://github.com/kgu/project", new ObjectMapper().readTree("[]"));
    }
}
