package projectApproval.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import kgu.developers.domain.project.domain.Project;
import kgu.developers.domain.project.domain.ProjectRepository;
import kgu.developers.domain.project.exception.ProjectNotFoundException;
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

    private static final Long PROJECT_ID = 1L;
    private static final String STUDENT_NUMBER = "202699999";

    @Mock
    private ProjectApprovalRepository projectApprovalRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ProjectApprovalCommandService projectApprovalCommandService;

    private final Project project = Project.builder().id(PROJECT_ID).build();

    private final User student = User.create(STUDENT_NUMBER, "kgu@kyonggi.ac.kr", "김철수", "encoded",
            UserGlobalRole.USER, "010-1234-6789");

    @Test
    @DisplayName("동의 이력이 없으면 새 동의를 생성한다")
    void createProjectApproval() {
        LocalDateTime approvedAt = LocalDateTime.of(2026, 1, 15, 10, 0);
        given(projectRepository.findById(PROJECT_ID)).willReturn(Optional.of(project));
        given(userRepository.findByStudentNumber(STUDENT_NUMBER)).willReturn(Optional.of(student));
        given(projectApprovalRepository.findIncludingDeleted(PROJECT_ID, STUDENT_NUMBER))
                .willReturn(Optional.empty());
        given(projectApprovalRepository.save(any(ProjectApproval.class)))
                .willReturn(ProjectApproval.builder().id(10L).build());

        Long id = projectApprovalCommandService.createProjectApproval(PROJECT_ID, STUDENT_NUMBER, approvedAt);

        assertThat(id).isEqualTo(10L);
        ArgumentCaptor<ProjectApproval> captor = ArgumentCaptor.forClass(ProjectApproval.class);
        verify(projectApprovalRepository).save(captor.capture());
        assertThat(captor.getValue().getProjectId()).isEqualTo(PROJECT_ID);
        assertThat(captor.getValue().getUserId()).isEqualTo(STUDENT_NUMBER);
        assertThat(captor.getValue().getApprovedAt()).isEqualTo(approvedAt);
        assertThat(captor.getValue().getDeletedAt()).isNull();
    }

    @Test
    @DisplayName("이미 활성 상태로 동의한 사용자면 DuplicateProjectApprovalException을 던진다")
    void createProjectApprovalThrowsOnActiveDuplicate() {
        given(projectRepository.findById(PROJECT_ID)).willReturn(Optional.of(project));
        given(userRepository.findByStudentNumber(STUDENT_NUMBER)).willReturn(Optional.of(student));
        given(projectApprovalRepository.findIncludingDeleted(PROJECT_ID, STUDENT_NUMBER))
                .willReturn(Optional.of(ProjectApproval.builder()
                        .id(10L)
                        .projectId(PROJECT_ID)
                        .userId(STUDENT_NUMBER)
                        .approvedAt(LocalDateTime.of(2026, 1, 15, 10, 0))
                        .build()));

        assertThatThrownBy(() -> projectApprovalCommandService.createProjectApproval(
                PROJECT_ID, STUDENT_NUMBER, LocalDateTime.of(2026, 3, 1, 9, 0)))
                .isInstanceOf(DuplicateProjectApprovalException.class);

        verify(projectApprovalRepository, never()).save(any(ProjectApproval.class));
    }

    @Test
    @DisplayName("소프트 삭제된 동의가 있으면 기존 행을 재활성화한다")
    void createProjectApprovalReactivatesSoftDeleted() {
        LocalDateTime approvedAt = LocalDateTime.of(2026, 3, 1, 9, 0);
        ProjectApproval deleted = ProjectApproval.builder()
                .id(10L)
                .projectId(PROJECT_ID)
                .userId(STUDENT_NUMBER)
                .approvedAt(LocalDateTime.of(2026, 1, 15, 10, 0))
                .deletedAt(LocalDateTime.of(2026, 2, 1, 9, 0))
                .build();
        given(projectRepository.findById(PROJECT_ID)).willReturn(Optional.of(project));
        given(userRepository.findByStudentNumber(STUDENT_NUMBER)).willReturn(Optional.of(student));
        given(projectApprovalRepository.findIncludingDeleted(PROJECT_ID, STUDENT_NUMBER))
                .willReturn(Optional.of(deleted));
        given(projectApprovalRepository.save(any(ProjectApproval.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        Long id = projectApprovalCommandService.createProjectApproval(PROJECT_ID, STUDENT_NUMBER, approvedAt);

        assertThat(id).isEqualTo(10L);
        assertThat(deleted.getDeletedAt()).isNull();
        assertThat(deleted.getApprovedAt()).isEqualTo(approvedAt);
    }

    @Test
    @DisplayName("존재하지 않는 프로젝트면 ProjectNotFoundException을 던진다")
    void createProjectApprovalThrowsOnUnknownProject() {
        given(projectRepository.findById(PROJECT_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> projectApprovalCommandService.createProjectApproval(
                PROJECT_ID, STUDENT_NUMBER, LocalDateTime.of(2026, 1, 15, 10, 0)))
                .isInstanceOf(ProjectNotFoundException.class);

        verify(projectApprovalRepository, never()).save(any(ProjectApproval.class));
    }

    @Test
    @DisplayName("존재하지 않는 사용자면 UserNotFoundException을 던진다")
    void createProjectApprovalThrowsOnUnknownUser() {
        given(projectRepository.findById(PROJECT_ID)).willReturn(Optional.of(project));
        given(userRepository.findByStudentNumber(STUDENT_NUMBER)).willReturn(Optional.empty());

        assertThatThrownBy(() -> projectApprovalCommandService.createProjectApproval(
                PROJECT_ID, STUDENT_NUMBER, LocalDateTime.of(2026, 1, 15, 10, 0)))
                .isInstanceOf(UserNotFoundException.class);

        verify(projectApprovalRepository, never()).save(any(ProjectApproval.class));
    }
}
