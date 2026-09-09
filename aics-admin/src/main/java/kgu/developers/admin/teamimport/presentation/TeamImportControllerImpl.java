package kgu.developers.admin.teamimport.presentation;

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
import kgu.developers.admin.teamimport.application.TeamImportFacade;
import kgu.developers.admin.teamimport.presentation.response.TeamImportApplyResponse;
import kgu.developers.admin.teamimport.presentation.response.TeamImportPreviewResponse;
import lombok.RequiredArgsConstructor;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin")
public class TeamImportControllerImpl implements TeamImportController {

    private final TeamImportFacade teamImportFacade;

    @Override
    @PostMapping(value = "/sections/{sectionId}/team-imports/preview", consumes = MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TeamImportPreviewResponse> preview(
        @Positive @PathVariable Long sectionId,
        @RequestPart MultipartFile file) {
        String studentNumber = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(teamImportFacade.preview(sectionId, studentNumber, file));
    }

    @Override
    @PostMapping("/team-imports/{importId}/apply")
    public ResponseEntity<TeamImportApplyResponse> apply(
        @Positive @PathVariable Long importId) {
        String studentNumber = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(teamImportFacade.apply(importId, studentNumber));
    }
}
