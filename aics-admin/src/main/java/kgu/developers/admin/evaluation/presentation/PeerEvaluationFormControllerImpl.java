package kgu.developers.admin.evaluation.presentation;

import kgu.developers.admin.evaluation.application.PeerEvaluationFormFacade;
import kgu.developers.admin.evaluation.presentation.request.PeerEvaluationFormCreateRequest;
import kgu.developers.admin.evaluation.presentation.request.PeerEvaluationFormUpdateRequest;
import kgu.developers.admin.evaluation.presentation.response.PeerEvaluationFormPersistResponse;
import kgu.developers.admin.evaluation.presentation.response.PeerEvaluationFormResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/sections/{sectionId}/peer-evaluation-forms")
public class PeerEvaluationFormControllerImpl implements PeerEvaluationFormController {

    private final PeerEvaluationFormFacade facade;

    @Override
    @PostMapping
    public ResponseEntity<PeerEvaluationFormPersistResponse> createForm(
            @PathVariable Long sectionId,
            @RequestBody PeerEvaluationFormCreateRequest request,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(facade.createForm(sectionId, authentication.getName(), request));
    }

    @Override
    @GetMapping("/{formId}")
    public ResponseEntity<PeerEvaluationFormResponse> getForm(
            @PathVariable Long sectionId,
            @PathVariable Long formId,
            Authentication authentication) {
        return ResponseEntity.ok(facade.getForm(sectionId, authentication.getName(), formId));
    }

    @Override
    @GetMapping("/milestones/{milestoneId}")
    public ResponseEntity<PeerEvaluationFormResponse> getFormByMilestoneId(
            @PathVariable Long sectionId,
            @PathVariable Long milestoneId,
            Authentication authentication) {
        return ResponseEntity.ok(facade.getFormByMilestoneId(sectionId, authentication.getName(), milestoneId));
    }

    @Override
    @PutMapping("/{formId}")
    public ResponseEntity<Void> updateForm(
            @PathVariable Long sectionId,
            @PathVariable Long formId,
            @RequestBody PeerEvaluationFormUpdateRequest request,
            Authentication authentication) {
        facade.updateForm(sectionId, authentication.getName(), formId, request);
        return ResponseEntity.noContent().build();
    }

    @Override
    @PutMapping("/milestones/{milestoneId}")
    public ResponseEntity<Void> updateFormByMilestoneId(
            @PathVariable Long sectionId,
            @PathVariable Long milestoneId,
            @RequestBody PeerEvaluationFormUpdateRequest request,
            Authentication authentication) {
        facade.updateFormByMilestoneId(sectionId, authentication.getName(), milestoneId, request);
        return ResponseEntity.noContent().build();
    }
}
