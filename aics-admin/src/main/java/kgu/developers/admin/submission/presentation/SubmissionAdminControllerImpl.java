package kgu.developers.admin.submission.presentation;

import java.nio.charset.StandardCharsets;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import kgu.developers.admin.submission.application.SubmissionAdminFacade;
import kgu.developers.admin.submission.application.SubmissionArtifactsZipDownload;
import kgu.developers.admin.submission.presentation.response.SubmissionAdminListResponse;
import kgu.developers.admin.submission.presentation.response.SubmissionAdminResponse;
import kgu.developers.admin.submission.presentation.response.SubmissionVersionAdminDetailResponse;
import kgu.developers.admin.submission.presentation.response.SubmissionVersionAdminListResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/oop")
public class SubmissionAdminControllerImpl implements SubmissionAdminController {

    private final SubmissionAdminFacade submissionAdminFacade;

    @Override
    @GetMapping("/milestones/{milestoneId}/submissions")
    public ResponseEntity<SubmissionAdminListResponse> getSubmissionsByMilestone(
        @PathVariable Long milestoneId,
        Authentication authentication
    ) {
        return ResponseEntity.ok(
            submissionAdminFacade.getSubmissionsByMilestone(milestoneId, authentication.getName()));
    }

    @Override
    @GetMapping("/submissions/{submissionId}")
    public ResponseEntity<SubmissionAdminResponse> getSubmission(
        @PathVariable Long submissionId,
        Authentication authentication
    ) {
        return ResponseEntity.ok(submissionAdminFacade.getSubmission(submissionId, authentication.getName()));
    }

    @Override
    @GetMapping("/submissions/{submissionId}/versions")
    public ResponseEntity<SubmissionVersionAdminListResponse> getVersions(
        @PathVariable Long submissionId,
        Authentication authentication
    ) {
        return ResponseEntity.ok(submissionAdminFacade.getVersions(submissionId, authentication.getName()));
    }

    @Override
    @GetMapping("/submissions/{submissionId}/versions/{version}")
    public ResponseEntity<SubmissionVersionAdminDetailResponse> getVersion(
        @PathVariable Long submissionId,
        @PathVariable int version,
        Authentication authentication
    ) {
        return ResponseEntity.ok(submissionAdminFacade.getVersion(submissionId, version, authentication.getName()));
    }

    @Override
    @GetMapping("/submissions/{submissionId}/download")
    public ResponseEntity<StreamingResponseBody> downloadArtifacts(
        @PathVariable Long submissionId,
        Authentication authentication
    ) {
        SubmissionArtifactsZipDownload download = submissionAdminFacade
            .downloadArtifactsZip(submissionId, authentication.getName());
        ContentDisposition contentDisposition = ContentDisposition.attachment()
            .filename(download.fileName(), StandardCharsets.UTF_8)
            .build();
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
            .contentType(MediaType.valueOf("application/zip"))
            .body(download.body());
    }
}
