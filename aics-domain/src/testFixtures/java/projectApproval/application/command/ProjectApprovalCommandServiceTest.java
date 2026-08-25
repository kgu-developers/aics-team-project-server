package projectApproval.application.command;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import kgu.developers.common.exception.CustomException;
import kgu.developers.domain.projectApproval.application.command.ProjectApprovalCommandService;
import kgu.developers.domain.projectApproval.domain.ProjectApprovalRepository;
import kgu.developers.domain.project.domain.ApprovalStatus;
import kgu.developers.domain.project.domain.Project;
import kgu.developers.domain.project.domain.ProjectRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class ProjectApprovalCommandServiceTest {

    @Mock private ProjectApprovalRepository projectApprovalRepository;
    @Mock private ProjectRepository projectRepository;
    @InjectMocks private ProjectApprovalCommandService projectApprovalCommandService;

    @Test
    @DisplayName("approve는 본인 동의 기록을 생성한다")
    void approve() {
        givenProject();
        given(projectApprovalRepository.existsByProjectIdAndUserIdAndProposalRevision(10L, "202412345", 0L)).willReturn(false);

        projectApprovalCommandService.approve(10L, "202412345");

        ArgumentCaptor<kgu.developers.domain.projectApproval.domain.ProjectApproval> captor = ArgumentCaptor.forClass(kgu.developers.domain.projectApproval.domain.ProjectApproval.class);
        then(projectApprovalRepository).should().save(captor.capture());
        org.assertj.core.api.Assertions.assertThat(captor.getValue().getProjectId()).isEqualTo(10L);
        org.assertj.core.api.Assertions.assertThat(captor.getValue().getUserId()).isEqualTo("202412345");
        org.assertj.core.api.Assertions.assertThat(captor.getValue().getProposalRevision()).isZero();
    }

    @Test
    @DisplayName("approve는 이미 동의한 사용자면 예외를 던진다")
    void approve_rejectsDuplicate() {
        givenProject();
        given(projectApprovalRepository.existsByProjectIdAndUserIdAndProposalRevision(10L, "202412345", 0L)).willReturn(true);

        assertThatThrownBy(() -> projectApprovalCommandService.approve(10L, "202412345"))
            .isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("approve는 동시 요청으로 유니크 제약이 충돌해도 중복 동의 예외로 변환한다")
    void approve_convertsUniqueConstraintViolation() {
        givenProject();
        given(projectApprovalRepository.existsByProjectIdAndUserIdAndProposalRevision(10L, "202412345", 0L)).willReturn(false);
        given(projectApprovalRepository.save(org.mockito.ArgumentMatchers.any()))
            .willThrow(new DataIntegrityViolationException("duplicate"));

        assertThatThrownBy(() -> projectApprovalCommandService.approve(10L, "202412345"))
            .isInstanceOf(CustomException.class);
    }

    private void givenProject() {
        given(projectRepository.findByIdForUpdate(10L)).willReturn(Optional.of(
            Project.builder().id(10L).teamId(1L).title("제목").description("설명").goal("목표")
                .approvalStatus(ApprovalStatus.DRAFT).build()
        ));
    }
}
