package kgu.developers.api.submission.presentation;

import java.util.List;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import kgu.developers.api.submission.application.SubmissionFacade;
import kgu.developers.api.submission.presentation.request.PresentationContentRequest;
import kgu.developers.api.submission.presentation.request.PresentationOrderRequest;
import kgu.developers.api.submission.presentation.request.SubmissionArtifactRequest;
import kgu.developers.api.submission.presentation.request.SubmissionMemberConfirmationRequest;
import kgu.developers.api.submission.presentation.request.SubmissionReopenRequest;
import kgu.developers.api.submission.presentation.response.MilestonePresentationsResponse;
import kgu.developers.api.submission.presentation.response.PresentationContentResponse;
import kgu.developers.api.submission.presentation.response.SubmissionMemberConfirmationListResponse;
import kgu.developers.api.submission.presentation.response.SubmissionResponse;
import kgu.developers.api.submission.presentation.response.SubmissionVersionDetailResponse;
import kgu.developers.api.submission.presentation.response.SubmissionVersionListResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class SubmissionControllerImpl implements SubmissionController {

    private final SubmissionFacade submissionFacade;

    @Override
    @GetMapping("/milestones/{milestoneId}/my-team-submission")
    public ResponseEntity<SubmissionResponse> getMyTeamSubmission(@PathVariable Long milestoneId, Authentication authentication) {
        return ResponseEntity.ok(submissionFacade.getMyTeamSubmission(milestoneId, authentication.getName()));
    }

    @Override
    @GetMapping("/submissions/{submissionId}")
    public ResponseEntity<SubmissionResponse> getSubmission(@PathVariable Long submissionId, Authentication authentication) {
        return ResponseEntity.ok(submissionFacade.getSubmission(submissionId, authentication.getName()));
    }

    @Override
    @GetMapping("/submissions/{submissionId}/versions")
    public ResponseEntity<SubmissionVersionListResponse> getVersions(@PathVariable Long submissionId, Authentication authentication) {
        return ResponseEntity.ok(submissionFacade.getVersions(submissionId, authentication.getName()));
    }

    @Override
    @GetMapping("/submissions/{submissionId}/versions/{version}")
    public ResponseEntity<SubmissionVersionDetailResponse> getVersion(
        @PathVariable Long submissionId,
        @PathVariable int version,
        Authentication authentication
    ) {
        return ResponseEntity.ok(submissionFacade.getVersion(submissionId, version, authentication.getName()));
    }

    @Override
    @PostMapping(value = "/submissions/{submissionId}/versions", consumes = "multipart/form-data")
    public ResponseEntity<SubmissionResponse> submitVersion(
        @PathVariable Long submissionId,
        @RequestParam String description,
        @RequestParam(required = false) String changeNote,
        @RequestPart(value = "artifacts", required = false) @Valid List<SubmissionArtifactRequest> artifacts,
        @RequestParam(required = false) List<Long> fileArtifactIds,
        @RequestPart(value = "files", required = false) List<MultipartFile> files,
        Authentication authentication
    ) {
        return ResponseEntity.ok(submissionFacade.submitVersion(
            submissionId, authentication.getName(), description, changeNote, artifacts, fileArtifactIds, files));
    }

    @Override
    @GetMapping("/submissions/{submissionId}/member-confirmations")
    public ResponseEntity<SubmissionMemberConfirmationListResponse> getMemberConfirmations(
        @PathVariable Long submissionId,
        Authentication authentication
    ) {
        return ResponseEntity.ok(submissionFacade.getMemberConfirmations(submissionId, authentication.getName()));
    }

    @Override
    @PostMapping("/submissions/{submissionId}/member-confirmations")
    public ResponseEntity<Void> confirmAsMember(
        @PathVariable Long submissionId,
        @Valid @RequestBody SubmissionMemberConfirmationRequest request,
        Authentication authentication
    ) {
        submissionFacade.confirmAsMember(submissionId, authentication.getName(), request);
        return ResponseEntity.noContent().build();
    }

    @Override
    @PatchMapping("/submissions/{submissionId}/complete")
    public ResponseEntity<SubmissionResponse> completeSubmission(@PathVariable Long submissionId, Authentication authentication) {
        return ResponseEntity.ok(submissionFacade.completeSubmission(submissionId, authentication.getName()));
    }

    @Override
    @PatchMapping("/submissions/{submissionId}/reopen")
    public ResponseEntity<SubmissionResponse> reopenSubmission(
        @PathVariable Long submissionId,
        @Valid @RequestBody SubmissionReopenRequest request,
        Authentication authentication
    ) {
        return ResponseEntity.ok(submissionFacade.reopenSubmission(submissionId, authentication.getName(), request));
    }

    @Override
    @GetMapping("/submissions/{submissionId}/presentation-content")
    public ResponseEntity<PresentationContentResponse> getPresentationContent(
        @PathVariable Long submissionId, Authentication authentication
    ) {
        return ResponseEntity.ok(submissionFacade.getPresentationContent(submissionId, authentication.getName()));
    }

    @Override
    @PutMapping("/submissions/{submissionId}/presentation-content")
    public ResponseEntity<PresentationContentResponse> updatePresentationContent(
        @PathVariable Long submissionId,
        @RequestBody PresentationContentRequest request,
        Authentication authentication
    ) {
        return ResponseEntity.ok(submissionFacade.updatePresentationContent(submissionId, authentication.getName(), request));
    }

    @Override
    @GetMapping("/milestones/{milestoneId}/presentations")
    public ResponseEntity<MilestonePresentationsResponse> getMilestonePresentations(
        @PathVariable Long milestoneId, Authentication authentication
    ) {
        return ResponseEntity.ok(submissionFacade.getMilestonePresentations(milestoneId, authentication.getName()));
    }

    @Override
    @PatchMapping("/milestones/{milestoneId}/presentation-order")
    public ResponseEntity<Void> assignPresentationOrder(
        @PathVariable Long milestoneId,
        @Valid @RequestBody PresentationOrderRequest request,
        Authentication authentication
    ) {
        submissionFacade.assignPresentationOrder(milestoneId, authentication.getName(), request);
        return ResponseEntity.noContent().build();
    }
}
