package kgu.developers.api.auditlog.presentation;

import jakarta.validation.constraints.Positive;
import kgu.developers.api.auditlog.application.AuditLogFacade;
import kgu.developers.api.auditlog.presentation.response.TeamActivitySummaryResponse;
import kgu.developers.api.auditlog.presentation.response.TeamHistoryPageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/oop")
public class AuditLogControllerImpl implements AuditLogController {

    private final AuditLogFacade auditLogFacade;

    @Override
    @GetMapping("/teams/{teamId}/histories")
    public ResponseEntity<TeamHistoryPageResponse> getTeamHistories(
            @Positive @PathVariable Long teamId,
            @PageableDefault(sort = {"createdAt", "id"}, direction = Sort.Direction.DESC) Pageable pageable,
            Authentication authentication
    ) {
        return ResponseEntity.ok(auditLogFacade.getTeamHistories(teamId, pageable, authentication.getName()));
    }

    @Override
    @GetMapping("/teams/{teamId}/activity-summary")
    public ResponseEntity<TeamActivitySummaryResponse> getTeamActivitySummary(
            @Positive @PathVariable Long teamId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(auditLogFacade.getTeamActivitySummary(teamId, authentication.getName()));
    }
}
