package kgu.developers.admin.evaluation.presentation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import kgu.developers.admin.evaluation.application.PeerEvaluationFormFacade;
import kgu.developers.admin.evaluation.presentation.request.PeerEvaluationFormCreateRequest;
import kgu.developers.admin.evaluation.presentation.response.PeerEvaluationFormPersistResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/oop/sections/{sectionId}/peer-evaluation-forms")
public class PeerEvaluationFormControllerImpl implements PeerEvaluationFormController {

    private final PeerEvaluationFormFacade facade;

    @Override
    @PostMapping
    public ResponseEntity<PeerEvaluationFormPersistResponse> createForm(
            @Positive @PathVariable Long sectionId,
            @Valid @RequestBody PeerEvaluationFormCreateRequest request,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(facade.createForm(sectionId, authentication.getName(), request));
    }
}
