package kgu.developers.api.milestone.presentation;

import kgu.developers.api.milestone.application.MilestoneFacade;
import kgu.developers.api.milestone.presentation.response.MilestoneListResponse;
import kgu.developers.api.milestone.presentation.response.RequiredArtifactListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class MilestoneControllerImpl implements MilestoneController {

    private final MilestoneFacade milestoneFacade;

    @Override
    @GetMapping("/sections/{sectionId}/milestones")
    public ResponseEntity<MilestoneListResponse> getMilestones(
        @PathVariable Long sectionId,
        Authentication authentication
    ) {
        return ResponseEntity.ok(milestoneFacade.getMilestones(sectionId, authentication.getName()));
    }

    @Override
    @GetMapping("/sections/{sectionId}/milestones/{milestoneId}/required-artifacts")
    public ResponseEntity<RequiredArtifactListResponse> getRequiredArtifacts(
        @PathVariable Long sectionId,
        @PathVariable Long milestoneId,
        Authentication authentication
    ) {
        return ResponseEntity.ok(milestoneFacade.getRequiredArtifacts(
            sectionId,
            milestoneId,
            authentication.getName()
        ));
    }
}
