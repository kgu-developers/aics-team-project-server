package kgu.developers.admin.evaluation.presentation;

import kgu.developers.admin.evaluation.application.PeerEvaluationAdminFacade;
import kgu.developers.admin.evaluation.presentation.response.PeerEvaluationAdminListResponse;
import kgu.developers.admin.evaluation.presentation.response.PeerEvaluationAdminTeamDetailResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/oop/sections/{sectionId}/peer-evaluations")
public class PeerEvaluationAdminControllerImpl implements PeerEvaluationAdminController {

    private final PeerEvaluationAdminFacade facade;

    @Override
    @GetMapping
    public ResponseEntity<PeerEvaluationAdminListResponse> getPeerEvaluations(
            @PathVariable Long sectionId,
            @RequestParam(required = false) Long formId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(facade.getPeerEvaluations(sectionId, formId, authentication.getName()));
    }

    @Override
    @GetMapping("/teams/{teamId}")
    public ResponseEntity<PeerEvaluationAdminTeamDetailResponse> getTeamPeerEvaluationDetail(
            @PathVariable Long sectionId,
            @PathVariable Long teamId,
            @RequestParam(required = false) Long formId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(facade.getTeamPeerEvaluationDetail(sectionId, teamId, formId, authentication.getName()));
    }
}
