package projectApproval.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import kgu.developers.domain.project.domain.ApprovalStatus;
import kgu.developers.domain.project.domain.Project;
import kgu.developers.domain.project.domain.ProjectRepository;
import kgu.developers.domain.project.exception.ProjectNotFoundException;
import kgu.developers.domain.project.exception.ProjectProposalCompletedException;
import kgu.developers.domain.projectApproval.application.command.ProjectApprovalCommandService;
import kgu.developers.domain.projectApproval.domain.ProjectApproval;
import kgu.developers.domain.projectApproval.domain.ProjectApprovalRepository;
import kgu.developers.domain.projectApproval.exception.DuplicateProjectApprovalException;
import kgu.developers.domain.user.domain.User;
import kgu.developers.domain.user.domain.UserGlobalRole;
import kgu.developers.domain.user.domain.UserRepository;
import kgu.developers.domain.user.exception.UserNotFoundException;

@ExtendWith(MockitoExtension.class)
class ProjectApprovalCommandServiceTest {

    private static final Long PROJECT_ID = 10L;
    private static final String STUDENT_NUMBER = "202412345";
    private static final LocalDateTime APPROVED_AT = LocalDateTime.of(2026, 1, 15, 10, 0);

    @Mock
    private ProjectApprovalRepository projectApprovalRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ProjectApprovalCommandService projectApprovalCommandService;

    private final User student = User.create(STUDENT_NUMBER, "kgu@kyonggi.ac.kr", "김철수", "encoded",
            UserGlobalRole.USER, "010-1234-6789");

    @Test
    @DisplayName("approve는 현재 제안서 리비전으로 동의 기록을 생성한다")
    void approve() {
        givenProject(2L, null);
        givenUser();
        given(projectApprovalRepository.findIncludingDeleted(PROJECT_ID, STUDENT_NUMBER, 2L))
                .willReturn(Optional.empty());
        given(projectApprovalRepository.save(any(ProjectApproval.class)))
                .willReturn(ProjectApproval.builder().id(100L).build());

        Long id = projectApprovalCommandService.approve(PROJECT_ID, STUDENT_NUMBER, APPROVED_AT);

        assertThat(id).isEqualTo(100L);
        ArgumentCaptor<ProjectApproval> captor = ArgumentCaptor.forClass(ProjectApproval.class);
        verify(projectApprovalRepository).save(captor.capture());
        assertThat(captor.getValue().getProjectId()).isEqualTo(PROJECT_ID);
        assertThat(captor.getValue().getUserId()).isEqualTo(STUDENT_NUMBER);
        assertThat(captor.getValue().getProposalRevision()).isEqualTo(2L);
        assertThat(captor.getValue().getApprovedAt()).isEqualTo(APPROVED_AT);
    }

    @Test
    @DisplayName("approve는 같은 리비전에 이미 살아있는 동의가 있으면 예외를 던진다")
    void approveRejectsActiveDuplicate() {
        givenProject(0L, null);
        givenUser();
        given(projectApprovalRepository.findIncludingDeleted(PROJECT_ID, STUDENT_NUMBER, 0L))
                .willReturn(Optional.of(ProjectApproval.builder()
                        .id(100L)
                        .projectId(PROJECT_ID)
                        .userId(STUDENT_NUMBER)
                        .approvedAt(APPROVED_AT)
                        .build()));

        assertThatThrownBy(() -> projectApprovalCommandService.approve(PROJECT_ID, STUDENT_NUMBER, APPROVED_AT))
                .isInstanceOf(DuplicateProjectApprovalException.class);

        verify(projectApprovalRepository, never()).save(any(ProjectApproval.class));
    }

    @Test
    @DisplayName("approve는 같은 리비전에서 무효화된 동의가 있으면 기존 행을 재활성화한다")
    void approveReactivatesSoftDeleted() {
        ProjectApproval deleted = ProjectApproval.builder()
                .id(100L)
                .projectId(PROJECT_ID)
                .userId(STUDENT_NUMBER)
                .proposalRevision(1L)
                .approvedAt(LocalDateTime.of(2026, 1, 1, 9, 0))
                .deletedAt(LocalDateTime.of(2026, 1, 10, 9, 0))
                .build();
        givenProject(1L, null);
        givenUser();
        given(projectApprovalRepository.findIncludingDeleted(PROJECT_ID, STUDENT_NUMBER, 1L))
                .willReturn(Optional.of(deleted));
        given(projectApprovalRepository.save(any(ProjectApproval.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        Long id = projectApprovalCommandService.approve(PROJECT_ID, STUDENT_NUMBER, APPROVED_AT);

        assertThat(id).isEqualTo(100L);
        assertThat(deleted.getDeletedAt()).isNull();
        assertThat(deleted.getApprovedAt()).isEqualTo(APPROVED_AT);
        assertThat(deleted.getProposalRevision()).isEqualTo(1L);
    }

    @Test
    @DisplayName("approve는 이전 리비전의 동의는 재활성화하지 않고 새 동의를 만든다")
    void approveDoesNotReuseOtherRevision() {
        givenProject(2L, null);
        givenUser();
        given(projectApprovalRepository.findIncludingDeleted(PROJECT_ID, STUDENT_NUMBER, 2L))
                .willReturn(Optional.empty());
        given(projectApprovalRepository.save(any(ProjectApproval.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        projectApprovalCommandService.approve(PROJECT_ID, STUDENT_NUMBER, APPROVED_AT);

        ArgumentCaptor<ProjectApproval> captor = ArgumentCaptor.forClass(ProjectApproval.class);
        verify(projectApprovalRepository).save(captor.capture());
        assertThat(captor.getValue().getId()).isNull();
        assertThat(captor.getValue().getProposalRevision()).isEqualTo(2L);
    }

    @Test
    @DisplayName("approve는 중복 확인 전에 프로젝트 행을 잠근다")
    void approveLocksProjectBeforeDuplicateCheck() {
        givenProject(0L, null);
        givenUser();
        given(projectApprovalRepository.findIncludingDeleted(PROJECT_ID, STUDENT_NUMBER, 0L))
                .willReturn(Optional.empty());
        given(projectApprovalRepository.save(any(ProjectApproval.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        projectApprovalCommandService.approve(PROJECT_ID, STUDENT_NUMBER, APPROVED_AT);

        InOrder inOrder = inOrder(projectRepository, projectApprovalRepository);
        inOrder.verify(projectRepository).findByIdForUpdate(PROJECT_ID);
        inOrder.verify(projectApprovalRepository).findIncludingDeleted(PROJECT_ID, STUDENT_NUMBER, 0L);
        inOrder.verify(projectApprovalRepository).save(any(ProjectApproval.class));
    }

    @Test
    @DisplayName("approve는 제안이 완료된 프로젝트면 예외를 던진다")
    void approveRejectsCompletedProposal() {
        givenProject(0L, LocalDateTime.of(2026, 1, 14, 10, 0));

        assertThatThrownBy(() -> projectApprovalCommandService.approve(PROJECT_ID, STUDENT_NUMBER, APPROVED_AT))
                .isInstanceOf(ProjectProposalCompletedException.class);

        verify(projectApprovalRepository, never()).save(any(ProjectApproval.class));
    }

    @Test
    @DisplayName("approve는 존재하지 않는 프로젝트면 ProjectNotFoundException을 던진다")
    void approveThrowsOnUnknownProject() {
        given(projectRepository.findByIdForUpdate(PROJECT_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> projectApprovalCommandService.approve(PROJECT_ID, STUDENT_NUMBER, APPROVED_AT))
                .isInstanceOf(ProjectNotFoundException.class);

        verify(projectApprovalRepository, never()).save(any(ProjectApproval.class));
    }

    @Test
    @DisplayName("approve는 존재하지 않는 사용자면 UserNotFoundException을 던진다")
    void approveThrowsOnUnknownUser() {
        givenProject(0L, null);
        given(userRepository.findByStudentNumber(STUDENT_NUMBER)).willReturn(Optional.empty());

        assertThatThrownBy(() -> projectApprovalCommandService.approve(PROJECT_ID, STUDENT_NUMBER, APPROVED_AT))
                .isInstanceOf(UserNotFoundException.class);

        verify(projectApprovalRepository, never()).save(any(ProjectApproval.class));
    }

    private void givenProject(long proposalRevision, LocalDateTime proposalCompletedAt) {
        given(projectRepository.findByIdForUpdate(PROJECT_ID)).willReturn(Optional.of(
                Project.builder()
                        .id(PROJECT_ID)
                        .teamId(1L)
                        .title("제목")
                        .description("설명")
                        .goal("목표")
                        .approvalStatus(ApprovalStatus.DRAFT)
                        .proposalRevision(proposalRevision)
                        .proposalCompletedAt(proposalCompletedAt)
                        .build()
        ));
    }

    private void givenUser() {
        given(userRepository.findByStudentNumber(STUDENT_NUMBER)).willReturn(Optional.of(student));
    }
}
