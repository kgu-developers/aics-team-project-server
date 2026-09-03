package projectApproval.application.command;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;

import jakarta.persistence.Table;
import kgu.developers.common.exception.CustomException;
import kgu.developers.domain.projectApproval.application.command.ProjectApprovalCommandService;
import kgu.developers.domain.projectApproval.domain.ProjectApprovalRepository;
import kgu.developers.domain.projectApproval.exception.DuplicateProjectApprovalException;
import kgu.developers.domain.projectApproval.infrastructure.ProjectApprovalJpaEntity;
import kgu.developers.domain.project.domain.ApprovalStatus;
import kgu.developers.domain.project.domain.Project;
import kgu.developers.domain.project.domain.ProjectRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.DataException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.sql.SQLException;
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
    @DisplayName("approve는 중복 확인 전에 프로젝트 행을 잠근다")
    void approve_locksProjectBeforeDuplicateCheck() {
        givenProject();
        given(projectApprovalRepository.existsByProjectIdAndUserIdAndProposalRevision(10L, "202412345", 0L)).willReturn(false);

        projectApprovalCommandService.approve(10L, "202412345");

        InOrder inOrder = inOrder(projectRepository, projectApprovalRepository);
        inOrder.verify(projectRepository).findByIdForUpdate(10L);
        inOrder.verify(projectApprovalRepository).existsByProjectIdAndUserIdAndProposalRevision(10L, "202412345", 0L);
        inOrder.verify(projectApprovalRepository).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("approve는 동의 유니크 제약 위반만 중복 동의 예외로 변환하고 원인을 보존한다")
    void approve_convertsUniqueApprovalViolation() {
        givenProject();
        given(projectApprovalRepository.existsByProjectIdAndUserIdAndProposalRevision(10L, "202412345", 0L)).willReturn(false);
        DataIntegrityViolationException violation = integrityViolation(uniqueApprovalConstraintName());
        given(projectApprovalRepository.save(org.mockito.ArgumentMatchers.any())).willThrow(violation);

        assertThatThrownBy(() -> projectApprovalCommandService.approve(10L, "202412345"))
            .isInstanceOf(DuplicateProjectApprovalException.class)
            .hasCause(violation);
    }

    @Test
    @DisplayName("approve는 다른 제약 위반은 그대로 전파한다")
    void approve_propagatesOtherConstraintViolations() {
        givenProject();
        given(projectApprovalRepository.existsByProjectIdAndUserIdAndProposalRevision(10L, "202412345", 0L)).willReturn(false);
        DataIntegrityViolationException violation = integrityViolation("project_approval_pkey");
        given(projectApprovalRepository.save(org.mockito.ArgumentMatchers.any())).willThrow(violation);

        assertThatThrownBy(() -> projectApprovalCommandService.approve(10L, "202412345"))
            .isSameAs(violation);
    }

    @Test
    @DisplayName("approve는 user_id 길이 초과 같은 데이터 오류를 중복으로 위장하지 않는다")
    void approve_propagatesDataErrors() {
        givenProject();
        given(projectApprovalRepository.existsByProjectIdAndUserIdAndProposalRevision(10L, "202412345", 0L)).willReturn(false);
        DataIntegrityViolationException violation = new DataIntegrityViolationException(
            "value too long for type character varying(20)",
            new DataException("value too long", new SQLException())
        );
        given(projectApprovalRepository.save(org.mockito.ArgumentMatchers.any())).willThrow(violation);

        assertThatThrownBy(() -> projectApprovalCommandService.approve(10L, "202412345"))
            .isSameAs(violation);
    }

    private static String uniqueApprovalConstraintName() {
        return ProjectApprovalJpaEntity.class.getAnnotation(Table.class).uniqueConstraints()[0].name();
    }

    private static DataIntegrityViolationException integrityViolation(String constraintName) {
        return new DataIntegrityViolationException(
            "constraint [" + constraintName + "]",
            new ConstraintViolationException("violation", new SQLException(), constraintName)
        );
    }

    private void givenProject() {
        given(projectRepository.findByIdForUpdate(10L)).willReturn(Optional.of(
            Project.builder().id(10L).teamId(1L).title("제목").description("설명").goal("목표")
                .approvalStatus(ApprovalStatus.DRAFT).build()
        ));
    }
}
