package milestone.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import kgu.developers.api.milestone.application.MilestoneFacade;
import kgu.developers.api.milestone.presentation.response.MilestoneListResponse;
import kgu.developers.domain.enrollment.domain.Enrollment;
import kgu.developers.domain.enrollment.domain.EnrollmentRepository;
import kgu.developers.domain.enrollment.domain.Role;
import kgu.developers.domain.enrollment.domain.Status;
import kgu.developers.domain.milestone.application.query.MilestoneQueryService;
import kgu.developers.domain.milestone.domain.Milestone;
import kgu.developers.domain.milestone.domain.MilestoneSchedule;
import kgu.developers.domain.milestone.domain.MilestoneStatus;
import kgu.developers.domain.milestone.domain.MilestoneType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class MilestoneFacadeTest {

    private static final Long SECTION_ID = 1L;
    private static final String USER_ID = "202612345";

    @Mock
    private MilestoneQueryService milestoneQueryService;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @InjectMocks
    private MilestoneFacade milestoneFacade;

    @Test
    @DisplayName("활성 학생은 DRAFT를 제외한 분반 마일스톤을 주차순 조회한다")
    void getMilestones() {
        given(enrollmentRepository.findBySectionIdAndUserId(SECTION_ID, USER_ID))
            .willReturn(Optional.of(enrollment(Role.STUDENT, Status.ACTIVE)));
        given(milestoneQueryService.getMilestones(SECTION_ID, null))
            .willReturn(List.of(
                milestone(1L, 1, MilestoneStatus.DRAFT, MilestoneType.PROPOSAL),
                milestone(15L, 2, MilestoneStatus.PUBLISHED, MilestoneType.PRESENTATION),
                milestone(20L, 3, MilestoneStatus.CLOSED, MilestoneType.PEER_EVALUATION)
            ));

        MilestoneListResponse response = milestoneFacade.getMilestones(SECTION_ID, USER_ID);

        assertThat(response.contents()).extracting("id").containsExactly(15L, 20L);
        assertThat(response.contents().get(0).schedule().dueAt())
            .isEqualTo(LocalDateTime.of(2026, 11, 20, 23, 59));
        assertThat(response.contents().get(0).type()).isEqualTo(MilestoneType.PRESENTATION);
        verify(milestoneQueryService).getMilestones(SECTION_ID, null);
    }

    @Test
    @DisplayName("해당 분반 수강 정보가 없으면 마일스톤을 조회할 수 없다")
    void rejectUserWithoutEnrollment() {
        given(enrollmentRepository.findBySectionIdAndUserId(SECTION_ID, USER_ID))
            .willReturn(Optional.empty());

        assertThatThrownBy(() -> milestoneFacade.getMilestones(SECTION_ID, USER_ID))
            .isInstanceOf(AccessDeniedException.class)
            .hasMessage("해당 분반 수강생만 마일스톤을 조회할 수 있습니다.");
    }

    @Test
    @DisplayName("탈퇴한 학생은 마일스톤을 조회할 수 없다")
    void rejectWithdrawnStudent() {
        given(enrollmentRepository.findBySectionIdAndUserId(SECTION_ID, USER_ID))
            .willReturn(Optional.of(enrollment(Role.STUDENT, Status.WITHDRAWN)));

        assertThatThrownBy(() -> milestoneFacade.getMilestones(SECTION_ID, USER_ID))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("조교는 학생용 마일스톤 목록을 조회할 수 없다")
    void rejectAssistant() {
        given(enrollmentRepository.findBySectionIdAndUserId(SECTION_ID, USER_ID))
            .willReturn(Optional.of(enrollment(Role.ASSISTANT, Status.ACTIVE)));

        assertThatThrownBy(() -> milestoneFacade.getMilestones(SECTION_ID, USER_ID))
            .isInstanceOf(AccessDeniedException.class);
    }

    private Enrollment enrollment(Role role, Status status) {
        return Enrollment.create(SECTION_ID, USER_ID, role, status);
    }

    private Milestone milestone(Long id, int weekNumber, MilestoneStatus status, MilestoneType type) {
        return Milestone.restore(
            id,
            SECTION_ID,
            "마일스톤 " + weekNumber,
            "설명",
            weekNumber,
            status,
            new MilestoneSchedule(
                LocalDateTime.of(2026, 11, 10, 0, 0),
                LocalDateTime.of(2026, 11, 20, 23, 59),
                null,
                null,
                type == MilestoneType.PRESENTATION ? LocalDateTime.of(2026, 11, 21, 9, 0) : null,
                type == MilestoneType.PRESENTATION ? LocalDateTime.of(2026, 11, 21, 18, 0) : null
            ),
            type,
            false
        );
    }
}
