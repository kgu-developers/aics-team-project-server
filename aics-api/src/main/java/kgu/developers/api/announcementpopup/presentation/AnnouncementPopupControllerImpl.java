package kgu.developers.api.announcementpopup.presentation;

import static org.springframework.http.HttpStatus.CREATED;

import jakarta.validation.Valid;
import kgu.developers.api.announcementpopup.application.AnnouncementPopupFacade;
import kgu.developers.api.announcementpopup.presentation.request.AnnouncementPopupCreateRequest;
import kgu.developers.api.announcementpopup.presentation.request.AnnouncementPopupUpdateRequest;
import kgu.developers.api.announcementpopup.presentation.response.AnnouncementPopupListResponse;
import kgu.developers.api.announcementpopup.presentation.response.AnnouncementPopupResponse;
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
public class AnnouncementPopupControllerImpl implements AnnouncementPopupController {

    private final AnnouncementPopupFacade announcementPopupFacade;

    @Override
    @PostMapping(value = "/announcements/{announcementId}/popup", consumes = "multipart/form-data")
    public ResponseEntity<AnnouncementPopupResponse> createPopup(
        @PathVariable Long announcementId,
        @Valid @RequestPart("request") AnnouncementPopupCreateRequest request,
        @RequestPart(value = "image", required = false) MultipartFile image,
        Authentication authentication
    ) {
        return ResponseEntity.status(CREATED)
            .body(announcementPopupFacade.createPopup(announcementId, authentication.getName(), request, image));
    }

    @Override
    @PatchMapping(value = "/announcement-popups/{id}", consumes = "multipart/form-data")
    public ResponseEntity<AnnouncementPopupResponse> updatePopup(
        @PathVariable Long id,
        @Valid @RequestPart("request") AnnouncementPopupUpdateRequest request,
        @RequestPart(value = "image", required = false) MultipartFile image,
        Authentication authentication
    ) {
        return ResponseEntity.ok(announcementPopupFacade.updatePopup(id, authentication.getName(), request, image));
    }

    @Override
    @GetMapping("/sections/{sectionId}/announcement-popups")
    public ResponseEntity<AnnouncementPopupListResponse> getPopups(
        @PathVariable Long sectionId,
        Authentication authentication
    ) {
        return ResponseEntity.ok(announcementPopupFacade.getPopups(sectionId, authentication.getName()));
    }
}
