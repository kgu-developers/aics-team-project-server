package kgu.developers.admin.milestone.presentation;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import kgu.developers.admin.milestone.application.MilestoneFacade;
import kgu.developers.admin.milestone.presentation.request.MilestoneCreateRequest;
import kgu.developers.admin.milestone.presentation.request.MilestoneStatusRequest;
import kgu.developers.admin.milestone.presentation.request.MilestoneUpdateRequest;
import kgu.developers.admin.milestone.presentation.request.MilestoneWeekNumbersRequest;
import kgu.developers.admin.milestone.presentation.response.MilestoneListResponse;
import kgu.developers.admin.milestone.presentation.response.MilestonePersistResponse;
import kgu.developers.admin.milestone.presentation.response.MilestoneResponse;
import kgu.developers.domain.milestone.domain.MilestoneStatus;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/oop/sections/{sectionId}/milestones")
public class MilestoneControllerImpl implements MilestoneController {
    private final MilestoneFacade milestoneFacade;

    @Override
    @PostMapping
    public ResponseEntity<MilestonePersistResponse> createMilestone(
            @Positive @PathVariable Long sectionId,
            @Valid @RequestBody MilestoneCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(milestoneFacade.createMilestone(sectionId, request));
    }

    @Override
    @GetMapping
    public ResponseEntity<MilestoneListResponse> getMilestones(
            @Positive @PathVariable Long sectionId,
            @RequestParam(required = false) MilestoneStatus status
    ) {
        return ResponseEntity.ok(milestoneFacade.getMilestones(sectionId, status));
    }

    @Override
    @GetMapping("/{milestoneId}")
    public ResponseEntity<MilestoneResponse> getMilestone(
            @Positive @PathVariable Long sectionId,
            @Positive @PathVariable Long milestoneId
    ) {
        return ResponseEntity.ok(milestoneFacade.getMilestone(sectionId, milestoneId));
    }

    @Override
    @PutMapping("/{milestoneId}")
    public ResponseEntity<Void> updateMilestone(
            @Positive @PathVariable Long sectionId,
            @Positive @PathVariable Long milestoneId,
            @Valid @RequestBody MilestoneUpdateRequest request
    ) {
        milestoneFacade.updateMilestone(sectionId, milestoneId, request);
        return ResponseEntity.noContent().build();
    }

    @Override
    @PatchMapping("/{milestoneId}/status")
    public ResponseEntity<Void> changeStatus(
            @Positive @PathVariable Long sectionId,
            @Positive @PathVariable Long milestoneId,
            @Valid @RequestBody MilestoneStatusRequest request
    ) {
        milestoneFacade.changeStatus(sectionId, milestoneId, request);
        return ResponseEntity.noContent().build();
    }

    @Override
    @PutMapping("/week-numbers")
    public ResponseEntity<Void> updateWeekNumbers(
            @Positive @PathVariable Long sectionId,
            @Valid @RequestBody MilestoneWeekNumbersRequest request
    ) {
        milestoneFacade.updateWeekNumbers(sectionId, request);
        return ResponseEntity.noContent().build();
    }
}
