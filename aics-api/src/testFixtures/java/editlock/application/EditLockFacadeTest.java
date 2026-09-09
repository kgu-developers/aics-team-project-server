package editlock.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import kgu.developers.api.editlock.application.EditLockFacade;
import kgu.developers.api.editlock.presentation.request.EditLockAcquireRequest;
import kgu.developers.api.editlock.presentation.response.EditLockStatusResponse;
import kgu.developers.domain.editlock.application.command.EditLockCommandService;
import kgu.developers.domain.editlock.application.query.EditLockQueryService;
import kgu.developers.domain.editlock.domain.EditLockTargetType;
import kgu.developers.domain.enrollment.domain.Enrollment;
import kgu.developers.domain.enrollment.domain.Role;
import kgu.developers.domain.enrollment.domain.Status;
import kgu.developers.domain.project.domain.ApprovalStatus;
import kgu.developers.domain.project.domain.Project;
import kgu.developers.domain.project.exception.ProjectNotFoundException;
import kgu.developers.domain.team.domain.Team;
import kgu.developers.domain.teamMember.domain.TeamMember;
import mock.repository.FakeEditLockRepository;
import mock.repository.FakeEnrollmentRepository;
import mock.repository.FakeProjectRepository;
import mock.repository.FakeTeamMemberRepository;
import mock.repository.FakeTeamRepository;

public class EditLockFacadeTest {

    private static final String MEMBER = "202412345";
    private static final String OTHER_MEMBER = "202499999";
    private static final Long SECTION_ID = 10L;
    private static final Long TEAM_ID = 100L;
    private static final Long PROJECT_ID = 1L;
    private static final String SECTION_KEY = "DEFAULT";

    private EditLockFacade facade;
    private FakeProjectRepository projectRepository;
    private FakeTeamRepository teamRepository;
    private FakeTeamMemberRepository teamMemberRepository;
    private FakeEnrollmentRepository enrollmentRepository;

    @BeforeEach
    public void init() {
        FakeEditLockRepository fakeEditLockRepository = new FakeEditLockRepository();
        projectRepository = new FakeProjectRepository();
        teamRepository = new FakeTeamRepository();
        teamMemberRepository = new FakeTeamMemberRepository();
        enrollmentRepository = new FakeEnrollmentRepository();

        facade = new EditLockFacade(
            new EditLockCommandService(fakeEditLockRepository),
            new EditLockQueryService(fakeEditLockRepository),
            projectRepository,
            teamRepository,
            teamMemberRepository,
            enrollmentRepository
        );

        teamRepository.save(Team.builder()
            .id(TEAM_ID)
            .sectionId(SECTION_ID)
            .name("A팀")
            .build());

        teamMemberRepository.save(TeamMember.builder()
            .teamId(TEAM_ID)
            .userId(MEMBER)
            .build());

        enrollmentRepository.save(Enrollment.builder()
            .sectionId(SECTION_ID)
            .userId(MEMBER)
            .role(Role.STUDENT)
            .status(Status.ACTIVE)
            .build());

        projectRepository.save(Project.builder()
            .id(PROJECT_ID)
            .teamId(TEAM_ID)
            .title("테스트 프로젝트")
            .description("설명")
            .goal("목표")
            .approvalStatus(ApprovalStatus.PENDING)
            .build());
    }

    private EditLockAcquireRequest buildRequest() {
        return EditLockAcquireRequest.builder()
            .targetType(EditLockTargetType.PROJECT)
            .targetId(PROJECT_ID)
            .sectionKey(SECTION_KEY)
            .build();
    }

    @Test
    @DisplayName("해당 팀의 활성 학생은 프로젝트 편집 잠금을 획득할 수 있다")
    public void acquire_Success_WhenActiveTeamMember() {
        EditLockStatusResponse response = facade.acquire(MEMBER, buildRequest());

        assertThat(response.locked()).isTrue();
        assertThat(response.lockedBy()).isEqualTo(MEMBER);
    }

    @Test
    @DisplayName("잠금 상태를 조회하면 현재 편집자 정보를 반환한다")
    public void getStatus_Success_WhenLocked() {
        facade.acquire(MEMBER, buildRequest());

        EditLockStatusResponse status = facade.getStatus(EditLockTargetType.PROJECT, PROJECT_ID, SECTION_KEY, MEMBER);

        assertThat(status.locked()).isTrue();
        assertThat(status.lockedBy()).isEqualTo(MEMBER);
    }

    @Test
    @DisplayName("잠금을 해제하면 미잠금 상태가 된다")
    public void release_Success() {
        facade.acquire(MEMBER, buildRequest());
        facade.release(EditLockTargetType.PROJECT, PROJECT_ID, SECTION_KEY, MEMBER);

        EditLockStatusResponse status = facade.getStatus(EditLockTargetType.PROJECT, PROJECT_ID, SECTION_KEY, MEMBER);

        assertThat(status.locked()).isFalse();
    }

    @Test
    @DisplayName("팀 소속이 아닌 사용자는 프로젝트 잠금을 획득할 수 없다")
    public void acquire_RejectsNonMember() {
        enrollmentRepository.save(Enrollment.builder()
            .sectionId(SECTION_ID)
            .userId(OTHER_MEMBER)
            .role(Role.STUDENT)
            .status(Status.ACTIVE)
            .build());

        assertThatThrownBy(() -> facade.acquire(OTHER_MEMBER, buildRequest()))
            .isInstanceOf(AccessDeniedException.class)
            .hasMessage("그 팀 소속만 프로젝트를 편집할 수 있습니다.");
    }

    @Test
    @DisplayName("팀원 행이 있어도 활성 학생이 아니면 프로젝트 잠금을 획득할 수 없다")
    public void acquire_RejectsInactiveStudent() {
        enrollmentRepository.save(Enrollment.builder()
            .sectionId(SECTION_ID)
            .userId(OTHER_MEMBER)
            .role(Role.STUDENT)
            .status(Status.WITHDRAWN)
            .build());
        teamMemberRepository.save(TeamMember.builder()
            .teamId(TEAM_ID)
            .userId(OTHER_MEMBER)
            .build());

        assertThatThrownBy(() -> facade.acquire(OTHER_MEMBER, buildRequest()))
            .isInstanceOf(AccessDeniedException.class)
            .hasMessage("그 분반에 활성 학생으로 등록된 사용자만 편집할 수 있습니다.");
    }

    @Test
    @DisplayName("존재하지 않는 프로젝트 ID로 잠금을 획득하려 하면 ProjectNotFoundException이 발생한다")
    public void acquire_RejectsNonExistentProject() {
        EditLockAcquireRequest request = EditLockAcquireRequest.builder()
            .targetType(EditLockTargetType.PROJECT)
            .targetId(999999L)
            .sectionKey(SECTION_KEY)
            .build();

        assertThatThrownBy(() -> facade.acquire(MEMBER, request))
            .isInstanceOf(ProjectNotFoundException.class);
    }

    @Test
    @DisplayName("팀 소속이 아닌 사용자는 잠금 상태 조회도 거부된다")
    public void getStatus_RejectsNonMember() {
        enrollmentRepository.save(Enrollment.builder()
            .sectionId(SECTION_ID)
            .userId(OTHER_MEMBER)
            .role(Role.STUDENT)
            .status(Status.ACTIVE)
            .build());

        assertThatThrownBy(() -> facade.getStatus(EditLockTargetType.PROJECT, PROJECT_ID, SECTION_KEY, OTHER_MEMBER))
            .isInstanceOf(AccessDeniedException.class);
    }
}
