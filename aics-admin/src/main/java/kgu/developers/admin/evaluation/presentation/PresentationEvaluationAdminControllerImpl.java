package kgu.developers.admin.evaluation.presentation;

import kgu.developers.admin.evaluation.application.PresentationEvaluationAdminFacade;
import kgu.developers.admin.evaluation.application.ProfessorPresentationEvaluationFacade;
import kgu.developers.admin.evaluation.presentation.request.ProfessorPresentationEvaluationRequest;
import kgu.developers.admin.evaluation.presentation.response.ProfessorPresentationEvaluationResponse;
import kgu.developers.admin.evaluation.presentation.response.PresentationEvaluationAdminListResponse;
import kgu.developers.admin.evaluation.presentation.response.PresentationEvaluationAdminTeamDetailResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/sections/{sectionId}/presentation-evaluations")
public class PresentationEvaluationAdminControllerImpl implements PresentationEvaluationAdminController {

    private final PresentationEvaluationAdminFacade facade;
    private final ProfessorPresentationEvaluationFacade professorFacade;

    @Override
    @GetMapping
    public ResponseEntity<PresentationEvaluationAdminListResponse> getPresentationEvaluations(
            @PathVariable Long sectionId,
            @RequestParam(required = false) Long milestoneId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(facade.getPresentationEvaluations(sectionId, milestoneId, authentication.getName()));
    }

    @Override
    @GetMapping("/teams/{teamId}")
    public ResponseEntity<PresentationEvaluationAdminTeamDetailResponse> getTeamPresentationEvaluationDetail(
            @PathVariable Long sectionId,
            @PathVariable Long teamId,
            @RequestParam(required = false) Long milestoneId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(facade.getTeamPresentationEvaluationDetail(sectionId, teamId, milestoneId, authentication.getName()));
    }

    @Override
    @GetMapping("/{milestoneId}/teams/{teamId}/professor")
    public ResponseEntity<ProfessorPresentationEvaluationResponse> getProfessorEvaluation(
            @PathVariable Long sectionId, @PathVariable Long milestoneId,
            @PathVariable Long teamId, Authentication authentication) {
        return ResponseEntity.ok(professorFacade.getEvaluation(
                sectionId, milestoneId, teamId, authentication.getName()));
    }

    @Override
    @PutMapping("/{milestoneId}/teams/{teamId}/professor")
    public ResponseEntity<ProfessorPresentationEvaluationResponse> saveProfessorEvaluation(
            @PathVariable Long sectionId, @PathVariable Long milestoneId,
            @PathVariable Long teamId, @RequestBody ProfessorPresentationEvaluationRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(professorFacade.saveEvaluation(
                sectionId, milestoneId, teamId, authentication.getName(), request));
    }
}
