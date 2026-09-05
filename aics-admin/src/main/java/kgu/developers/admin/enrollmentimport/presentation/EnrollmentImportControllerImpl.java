package kgu.developers.admin.enrollmentimport.presentation;

import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.Positive;
import kgu.developers.admin.enrollmentimport.application.EnrollmentImportFacade;
import kgu.developers.admin.enrollmentimport.presentation.response.EnrollmentImportApplyResponse;
import kgu.developers.admin.enrollmentimport.presentation.response.EnrollmentImportPreviewResponse;
import lombok.RequiredArgsConstructor;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin")
public class EnrollmentImportControllerImpl implements EnrollmentImportController {

    private final EnrollmentImportFacade enrollmentImportFacade;

    @Override
    @PostMapping(value = "/sections/{sectionId}/enrollment-imports/preview", consumes = MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<EnrollmentImportPreviewResponse> preview(
        @Positive @PathVariable Long sectionId,
        @RequestPart MultipartFile file) {
        String studentNumber = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(enrollmentImportFacade.preview(sectionId, studentNumber, file));
    }

    @Override
    @PostMapping("/enrollment-imports/{importId}/apply")
    public ResponseEntity<EnrollmentImportApplyResponse> apply(
        @Positive @PathVariable Long importId) {
        String studentNumber = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(enrollmentImportFacade.apply(importId, studentNumber));
    }
}
