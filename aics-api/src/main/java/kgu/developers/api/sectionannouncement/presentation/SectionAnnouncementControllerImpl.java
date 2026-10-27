package kgu.developers.api.sectionannouncement.presentation;

import static org.springframework.http.HttpStatus.CREATED;

import jakarta.validation.Valid;
import java.util.List;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
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
    @PostMapping(value = "/sections/{sectionId}/announcements", consumes = "multipart/form-data")
    public ResponseEntity<SectionAnnouncementResponse> createAnnouncement(
        @PathVariable Long sectionId,
        @Valid @RequestPart("request") SectionAnnouncementCreateRequest request,
        @RequestPart(value = "files", required = false) List<MultipartFile> files,
        Authentication authentication
    ) {
        return ResponseEntity.status(CREATED)
            .body(sectionAnnouncementFacade.createAnnouncement(sectionId, authentication.getName(), request, files));
    }

    @Override
    @PatchMapping(value = "/announcements/{id}", consumes = "multipart/form-data")
    public ResponseEntity<SectionAnnouncementResponse> updateAnnouncement(
        @PathVariable Long id,
        @Valid @RequestPart("request") SectionAnnouncementUpdateRequest request,
        @RequestPart(value = "files", required = false) List<MultipartFile> files,
        Authentication authentication
    ) {
        return ResponseEntity.ok(sectionAnnouncementFacade.updateAnnouncement(id, authentication.getName(), request, files));
    }
}
