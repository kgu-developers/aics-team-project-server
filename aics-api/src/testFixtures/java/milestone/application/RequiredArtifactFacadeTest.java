package milestone.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import kgu.developers.api.milestone.application.MilestoneFacade;
import kgu.developers.domain.enrollment.domain.Enrollment;
import kgu.developers.domain.enrollment.domain.EnrollmentRepository;
import kgu.developers.domain.enrollment.domain.Role;
import kgu.developers.domain.enrollment.domain.Status;
import kgu.developers.domain.feedback.application.query.RequiredArtifactQueryService;
import kgu.developers.domain.feedback.domain.RequiredArtifact;
import kgu.developers.domain.feedback.domain.RequiredArtifactType;
import kgu.developers.domain.milestone.application.query.MilestoneQueryService;
import kgu.developers.domain.milestone.domain.Milestone;
import kgu.developers.domain.milestone.domain.MilestoneSchedule;
import kgu.developers.domain.milestone.domain.MilestoneStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class RequiredArtifactFacadeTest {
    private static final Long SECTION_ID = 1L;
    private static final Long MILESTONE_ID = 2L;
    private static final String USER_ID = "202612345";

    @Mock MilestoneQueryService milestoneQueryService;
    @Mock EnrollmentRepository enrollmentRepository;
    @Mock RequiredArtifactQueryService requiredArtifactQueryService;

    @InjectMocks MilestoneFacade milestoneFacade;

    @Test
    @DisplayName("활성 학생은 공개 마일스톤의 제출 항목 식별자를 조회한다")
    void getRequiredArtifacts() {
        given(enrollmentRepository.findBySectionIdAndUserId(SECTION_ID, USER_ID))
                .willReturn(Optional.of(Enrollment.create(SECTION_ID, USER_ID, Role.STUDENT, Status.ACTIVE)));
        given(milestoneQueryService.getMilestone(SECTION_ID, MILESTONE_ID))
                .willReturn(milestone(MilestoneStatus.PUBLISHED));
        given(requiredArtifactQueryService.getRequiredArtifacts(MILESTONE_ID)).willReturn(List.of(
                RequiredArtifact.restore(
                        3L, MILESTONE_ID, RequiredArtifactType.FILE, "보고서", true,
                        "pdf", 20, null, null, null
                )
        ));

        assertThat(milestoneFacade.getRequiredArtifacts(
                SECTION_ID, MILESTONE_ID, USER_ID
        ).contents()).singleElement().satisfies(response -> {
            assertThat(response.id()).isEqualTo(3L);
            assertThat(response.type()).isEqualTo(RequiredArtifactType.FILE);
            assertThat(response.allowedExtensions()).containsExactly("pdf");
            assertThat(response.maxFileSizeMb()).isEqualTo(20);
        });
    }

    @Test
    @DisplayName("학생은 비공개 마일스톤의 제출 항목을 조회할 수 없다")
    void rejectDraftMilestone() {
        given(enrollmentRepository.findBySectionIdAndUserId(SECTION_ID, USER_ID))
                .willReturn(Optional.of(Enrollment.create(SECTION_ID, USER_ID, Role.STUDENT, Status.ACTIVE)));
        given(milestoneQueryService.getMilestone(SECTION_ID, MILESTONE_ID))
                .willReturn(milestone(MilestoneStatus.DRAFT));

        assertThatThrownBy(() -> milestoneFacade.getRequiredArtifacts(
                SECTION_ID, MILESTONE_ID, USER_ID
        )).isInstanceOf(AccessDeniedException.class);
    }

    private Milestone milestone(MilestoneStatus status) {
        return Milestone.restore(
                MILESTONE_ID,
                SECTION_ID,
                "보고서",
                null,
                2,
                status,
                new MilestoneSchedule(null, LocalDateTime.of(2026, 9, 10, 23, 59), null, null, null, null)
        );
    }
}
