package kgu.developers.api.announcementpopup.presentation;

import jakarta.validation.Valid;
import kgu.developers.api.announcementpopup.application.StudentAnnouncementPopupFacade;
import kgu.developers.api.announcementpopup.presentation.request.AnnouncementPopupDismissRequest;
import kgu.developers.api.announcementpopup.presentation.response.AnnouncementPopupDismissalResponse;
import kgu.developers.api.announcementpopup.presentation.response.StudentAnnouncementPopupListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/announcement-popups")
public class StudentAnnouncementPopupControllerImpl implements StudentAnnouncementPopupController {
    private final StudentAnnouncementPopupFacade facade;

    @Override
    @GetMapping
    public ResponseEntity<StudentAnnouncementPopupListResponse> getPopups(Authentication authentication) {
        return ResponseEntity.ok(facade.getPopups(authentication.getName()));
    }

    @Override
    @PutMapping("/{id}/dismissal")
    public ResponseEntity<AnnouncementPopupDismissalResponse> dismiss(
        @PathVariable Long id, @Valid @RequestBody AnnouncementPopupDismissRequest request,
        Authentication authentication
    ) {
        return ResponseEntity.ok(facade.dismiss(id, authentication.getName(), request.contentVersion()));
    }
}
