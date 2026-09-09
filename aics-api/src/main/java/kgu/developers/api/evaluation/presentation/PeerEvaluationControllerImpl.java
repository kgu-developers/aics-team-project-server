package kgu.developers.api.evaluation.presentation;

import jakarta.validation.Valid;
import kgu.developers.api.evaluation.application.PeerEvaluationFacade;
import kgu.developers.api.evaluation.presentation.request.PeerEvaluationResponseRequest;
import kgu.developers.api.evaluation.presentation.response.MyPeerEvaluationResponse;
import kgu.developers.api.evaluation.presentation.response.PeerEvaluationTargetsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/peer-evaluation-forms")
@RequiredArgsConstructor
public class PeerEvaluationControllerImpl implements PeerEvaluationController {
    private final PeerEvaluationFacade peerEvaluationFacade;

    @Override
    @GetMapping("/{formId}/targets")
    public ResponseEntity<PeerEvaluationTargetsResponse> getTargets(
        @PathVariable Long formId,
        Authentication authentication
    ) {
        return ResponseEntity.ok(peerEvaluationFacade.getTargets(formId, authentication.getName()));
    }

    @Override
    @PostMapping("/{formId}/responses")
    public ResponseEntity<MyPeerEvaluationResponse> submitResponse(
        @PathVariable Long formId,
        @Valid @RequestBody PeerEvaluationResponseRequest request,
        Authentication authentication
    ) {
        return ResponseEntity.ok(peerEvaluationFacade.submitResponse(formId, authentication.getName(), request));
    }
}
