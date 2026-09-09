package kgu.developers.api.milestone.application;

import kgu.developers.api.milestone.presentation.response.MilestoneListResponse;
import kgu.developers.api.milestone.presentation.response.RequiredArtifactListResponse;
import kgu.developers.domain.feedback.application.query.RequiredArtifactQueryService;
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
    private final RequiredArtifactQueryService requiredArtifactQueryService;

    public MilestoneListResponse getMilestones(Long sectionId, String userId) {
        validateActiveStudent(sectionId, userId);
        return MilestoneListResponse.from(
            milestoneQueryService.getMilestones(sectionId, null).stream()
                .filter(milestone -> milestone.getStatus() != MilestoneStatus.DRAFT)
                .toList()
        );
    }

    public RequiredArtifactListResponse getRequiredArtifacts(
            Long sectionId,
            Long milestoneId,
            String userId
    ) {
        validateActiveStudent(sectionId, userId);
        if (milestoneQueryService.getMilestone(sectionId, milestoneId).getStatus() == MilestoneStatus.DRAFT) {
            throw new AccessDeniedException("공개되지 않은 마일스톤은 조회할 수 없습니다.");
        }
        return RequiredArtifactListResponse.from(
                requiredArtifactQueryService.getRequiredArtifacts(milestoneId)
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
