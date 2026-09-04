package kgu.developers.api.sectionannouncement.presentation;

import static org.springframework.http.HttpStatus.CREATED;

import jakarta.validation.Valid;
import kgu.developers.api.sectionannouncement.application.SectionAnnouncementFacade;
import kgu.developers.api.sectionannouncement.presentation.request.SectionAnnouncementCreateRequest;
import kgu.developers.api.sectionannouncement.presentation.request.SectionAnnouncementUpdateRequest;
import kgu.developers.api.sectionannouncement.presentation.response.SectionAnnouncementListResponse;
import kgu.developers.api.sectionannouncement.presentation.response.SectionAnnouncementResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class SectionAnnouncementControllerImpl implements SectionAnnouncementController {

    private final SectionAnnouncementFacade sectionAnnouncementFacade;

    @Override
    @GetMapping("/sections/{sectionId}/announcements")
    public ResponseEntity<SectionAnnouncementListResponse> getAnnouncements(
        @PathVariable Long sectionId,
        Authentication authentication
    ) {
        return ResponseEntity.ok(sectionAnnouncementFacade.getAnnouncements(sectionId, authentication.getName()));
    }

    @Override
    @PostMapping("/sections/{sectionId}/announcements")
    public ResponseEntity<SectionAnnouncementResponse> createAnnouncement(
        @PathVariable Long sectionId,
        @Valid @RequestBody SectionAnnouncementCreateRequest request,
        Authentication authentication
    ) {
        return ResponseEntity.status(CREATED)
            .body(sectionAnnouncementFacade.createAnnouncement(sectionId, authentication.getName(), request));
    }

    @Override
    @PatchMapping("/announcements/{id}")
    public ResponseEntity<SectionAnnouncementResponse> updateAnnouncement(
        @PathVariable Long id,
        @Valid @RequestBody SectionAnnouncementUpdateRequest request,
        Authentication authentication
    ) {
        return ResponseEntity.ok(sectionAnnouncementFacade.updateAnnouncement(id, authentication.getName(), request));
    }
}
