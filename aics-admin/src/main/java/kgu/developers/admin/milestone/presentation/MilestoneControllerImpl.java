package kgu.developers.admin.milestone.presentation;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import kgu.developers.admin.milestone.application.MilestoneFacade;
import kgu.developers.admin.milestone.presentation.request.MilestoneCreateRequest;
import kgu.developers.admin.milestone.presentation.request.MilestoneEvaluationWindowRequest;
import kgu.developers.admin.milestone.presentation.request.MilestoneStatusRequest;
import kgu.developers.admin.milestone.presentation.request.MilestoneUpdateRequest;
import kgu.developers.admin.milestone.presentation.request.MilestoneWeekNumbersRequest;
import kgu.developers.admin.milestone.presentation.response.MilestoneListResponse;
import kgu.developers.admin.milestone.presentation.response.MilestonePersistResponse;
import kgu.developers.admin.milestone.presentation.response.MilestoneResponse;
import kgu.developers.domain.milestone.domain.MilestoneStatus;
import lombok.RequiredArgsConstructor;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/oop/sections/{sectionId}/milestones")
public class MilestoneControllerImpl implements MilestoneController {
    private final MilestoneFacade milestoneFacade;

    @Override
    @PostMapping
    public ResponseEntity<MilestonePersistResponse> createMilestone(
            @PathVariable Long sectionId,
            @RequestBody MilestoneCreateRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(milestoneFacade.createMilestone(sectionId, authentication.getName(), request));
    }

    @Override
    @GetMapping
    public ResponseEntity<MilestoneListResponse> getMilestones(
            @PathVariable Long sectionId,
            @RequestParam(required = false) MilestoneStatus status,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                milestoneFacade.getMilestones(sectionId, authentication.getName(), status));
    }

    @Override
    @GetMapping("/{milestoneId}")
    public ResponseEntity<MilestoneResponse> getMilestone(
            @PathVariable Long sectionId,
            @PathVariable Long milestoneId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                milestoneFacade.getMilestone(sectionId, authentication.getName(), milestoneId));
    }

    @Override
    @PutMapping("/{milestoneId}")
    public ResponseEntity<Void> updateMilestone(
            @PathVariable Long sectionId,
            @PathVariable Long milestoneId,
            @RequestBody MilestoneUpdateRequest request,
            Authentication authentication
    ) {
        milestoneFacade.updateMilestone(sectionId, authentication.getName(), milestoneId, request);
        return ResponseEntity.noContent().build();
    }

    @Override
    @PatchMapping("/{milestoneId}/status")
    public ResponseEntity<Void> changeStatus(
            @PathVariable Long sectionId,
            @PathVariable Long milestoneId,
            @RequestBody MilestoneStatusRequest request,
            Authentication authentication
    ) {
        milestoneFacade.changeStatus(sectionId, authentication.getName(), milestoneId, request);
        return ResponseEntity.noContent().build();
    }

    @Override
    @PatchMapping("/{milestoneId}/evaluation-window")
    public ResponseEntity<Void> updateEvaluationWindow(
            @PathVariable Long sectionId,
            @PathVariable Long milestoneId,
            @RequestBody MilestoneEvaluationWindowRequest request,
            Authentication authentication
    ) {
        milestoneFacade.updateEvaluationWindow(
                sectionId,
                authentication.getName(),
                milestoneId,
                request
        );
        return ResponseEntity.noContent().build();
    }

    @Override
    @PutMapping("/week-numbers")
    public ResponseEntity<Void> updateWeekNumbers(
            @PathVariable Long sectionId,
            @RequestBody MilestoneWeekNumbersRequest request,
            Authentication authentication
    ) {
        milestoneFacade.updateWeekNumbers(sectionId, authentication.getName(), request);
        return ResponseEntity.noContent().build();
    }
}
