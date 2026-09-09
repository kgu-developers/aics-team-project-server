package kgu.developers.admin.importstatus.presentation;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.constraints.Positive;
import kgu.developers.admin.importstatus.application.RosterImportStatusFacade;
import kgu.developers.admin.importstatus.presentation.response.RosterImportStatusResponse;
import lombok.RequiredArgsConstructor;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/oop")
public class RosterImportStatusControllerImpl implements RosterImportStatusController {

    private final RosterImportStatusFacade rosterImportStatusFacade;

    @Override
    @GetMapping("/sections/{sectionId}/roster-import-status")
    public ResponseEntity<RosterImportStatusResponse> getStatus(
        @Positive @PathVariable Long sectionId
    ) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(rosterImportStatusFacade.getStatus(sectionId, userId));
    }
}
