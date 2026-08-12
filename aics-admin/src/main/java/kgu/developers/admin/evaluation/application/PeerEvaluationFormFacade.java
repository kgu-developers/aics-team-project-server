package kgu.developers.admin.evaluation.application;

import kgu.developers.admin.evaluation.presentation.request.PeerEvaluationFormCreateRequest;
import kgu.developers.admin.evaluation.presentation.response.PeerEvaluationFormPersistResponse;
import kgu.developers.domain.evaluation.application.command.PeerEvaluationFormCommandService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PeerEvaluationFormFacade {

    private final PeerEvaluationFormCommandService commandService;

    public PeerEvaluationFormPersistResponse createForm(
            Long sectionId, PeerEvaluationFormCreateRequest request) {
        Long id = commandService.createForm(
                sectionId,
                request.milestoneId(),
                request.anonymous(),
                request.opensAt(),
                request.closesAt());
        return PeerEvaluationFormPersistResponse.of(id);
    }
}
