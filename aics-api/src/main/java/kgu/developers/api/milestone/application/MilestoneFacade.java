package kgu.developers.api.milestone.application;

import kgu.developers.api.milestone.presentation.response.MilestoneListResponse;
import kgu.developers.domain.enrollment.domain.Enrollment;
import kgu.developers.domain.enrollment.domain.EnrollmentRepository;
import kgu.developers.domain.milestone.application.query.MilestoneQueryService;
import kgu.developers.domain.milestone.domain.MilestoneStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MilestoneFacade {

    private final MilestoneQueryService milestoneQueryService;
    private final EnrollmentRepository enrollmentRepository;

    public MilestoneListResponse getMilestones(Long sectionId, String userId) {
        validateActiveStudent(sectionId, userId);
        return MilestoneListResponse.from(
            milestoneQueryService.getMilestones(sectionId, null).stream()
                .filter(milestone -> milestone.getStatus() != MilestoneStatus.DRAFT)
                .toList()
        );
    }

    private void validateActiveStudent(Long sectionId, String userId) {
        Enrollment enrollment = enrollmentRepository.findBySectionIdAndUserId(sectionId, userId)
            .orElseThrow(() -> new AccessDeniedException("해당 분반 수강생만 마일스톤을 조회할 수 있습니다."));

        if (!enrollment.isActiveStudent()) {
            throw new AccessDeniedException("해당 분반 수강생만 마일스톤을 조회할 수 있습니다.");
        }
    }
}
