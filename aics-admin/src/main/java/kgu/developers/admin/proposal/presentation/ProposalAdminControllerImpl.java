package kgu.developers.admin.proposal.presentation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import kgu.developers.admin.proposal.application.ProposalAdminFacade;
import kgu.developers.admin.proposal.presentation.request.ProposalFeedbackAdminRequest;
import kgu.developers.admin.proposal.presentation.response.ProposalFeedbackAdminPageResponse;
import kgu.developers.admin.proposal.presentation.response.ProposalFeedbackAdminResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/sections/{sectionId}/teams/{teamId}/proposal")
public class ProposalAdminControllerImpl implements ProposalAdminController {

    private final ProposalAdminFacade proposalAdminFacade;

    @Override
    @PostMapping("/feedback")
    public ResponseEntity<ProposalFeedbackAdminResponse> postFeedback(
        @PathVariable @Positive Long sectionId,
        @PathVariable @Positive Long teamId,
        @Valid @RequestBody ProposalFeedbackAdminRequest request,
        Authentication authentication
    ) {
        return ResponseEntity.ok(
            proposalAdminFacade.postFeedback(sectionId, teamId, request, authentication.getName())
        );
    }

    @Override
    @GetMapping("/feedbacks")
    public ResponseEntity<ProposalFeedbackAdminPageResponse> getFeedbacks(
        @PathVariable @Positive Long sectionId,
        @PathVariable @Positive Long teamId,
        @RequestParam(defaultValue = "0") @PositiveOrZero int page,
        @RequestParam(defaultValue = "20") @Positive @Max(100) int size,
        Authentication authentication
    ) {
        return ResponseEntity.ok(
            proposalAdminFacade.getFeedbacks(sectionId, teamId, PageRequest.of(page, size), authentication.getName())
        );
    }
}
