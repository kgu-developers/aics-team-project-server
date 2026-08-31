package kgu.developers.admin.evaluation.application;

import kgu.developers.admin.evaluation.presentation.request.PeerEvaluationFormCreateRequest;
import kgu.developers.admin.evaluation.presentation.response.PeerEvaluationFormPersistResponse;
import kgu.developers.domain.evaluation.application.command.PeerEvaluationFormCommandService;
import kgu.developers.domain.milestone.application.query.MilestoneQueryService;
import kgu.developers.domain.section.application.query.SectionQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class PeerEvaluationFormFacade {

    private final PeerEvaluationFormCommandService commandService;
    private final SectionQueryService sectionQueryService;
    private final MilestoneQueryService milestoneQueryService;

    public PeerEvaluationFormPersistResponse createForm(
            Long sectionId, String professorId, PeerEvaluationFormCreateRequest request) {
        validateSectionAccess(sectionId, professorId);
        milestoneQueryService.getMilestone(sectionId, request.milestoneId());
        Long id = commandService.createForm(
                sectionId,
                request.milestoneId(),
                request.anonymous(),
                request.opensAt(),
                request.closesAt());
        return PeerEvaluationFormPersistResponse.of(id);
    }

    private void validateSectionAccess(Long sectionId, String professorId) {
        String ownerId = sectionQueryService.getSectionById(sectionId).section().getProfessorId();
        if (!professorId.equals(ownerId)) {
            throw new AccessDeniedException("담당 분반의 상호평가 양식만 관리할 수 있습니다.");
        }
    }
}
