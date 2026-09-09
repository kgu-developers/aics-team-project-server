package kgu.developers.api.evaluation.presentation;

import kgu.developers.api.evaluation.application.PeerEvaluationFacade;
import kgu.developers.api.evaluation.presentation.response.EvaluationContextResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/sections/{sectionId}/evaluation-context")
@RequiredArgsConstructor
public class EvaluationContextControllerImpl implements EvaluationContextController {
    private final PeerEvaluationFacade peerEvaluationFacade;

    @Override
    @GetMapping
    public ResponseEntity<EvaluationContextResponse> getContext(
        @PathVariable Long sectionId,
        Authentication authentication
    ) {
        return ResponseEntity.ok(peerEvaluationFacade.getContext(sectionId, authentication.getName()));
    }
}
