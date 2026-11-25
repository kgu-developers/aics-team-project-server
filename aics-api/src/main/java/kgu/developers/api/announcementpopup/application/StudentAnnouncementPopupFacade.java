package kgu.developers.api.announcementpopup.application;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import kgu.developers.api.announcementpopup.presentation.response.AnnouncementPopupDismissalResponse;
import kgu.developers.api.announcementpopup.presentation.response.AnnouncementPopupResponse;
import kgu.developers.api.announcementpopup.presentation.response.StudentAnnouncementPopupListResponse;
import kgu.developers.domain.announcementpopup.domain.AnnouncementPopup;
import kgu.developers.domain.announcementpopup.domain.AnnouncementPopupDismissal;
import kgu.developers.domain.announcementpopup.domain.AnnouncementPopupDismissalRepository;
import kgu.developers.domain.announcementpopup.domain.StudentAnnouncementPopupRepository;
import kgu.developers.domain.announcementpopup.exception.AnnouncementPopupConcurrentlyModifiedException;
import kgu.developers.domain.announcementpopup.exception.AnnouncementPopupNotFoundException;
import kgu.developers.domain.fileobject.domain.FileObject;
import kgu.developers.domain.fileobject.domain.FileObjectRepository;
import kgu.developers.domain.fileobject.domain.FileStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class StudentAnnouncementPopupFacade {
    private final StudentAnnouncementPopupRepository popupRepository;
    private final AnnouncementPopupDismissalRepository dismissalRepository;
    private final FileObjectRepository fileObjectRepository;
    private final FileStorage fileStorage;
    private final Clock serviceClock;

    public StudentAnnouncementPopupListResponse getPopups(String userId) {
        LocalDateTime now = LocalDateTime.now(serviceClock);
        Map<Long, AnnouncementPopupDismissal> dismissals = dismissalRepository.findAllByUserId(userId).stream()
            .collect(Collectors.toMap(AnnouncementPopupDismissal::popupId, Function.identity()));
        List<AnnouncementPopup> popups = popupRepository.findAvailable(userId, null, now).stream()
            .filter(popup -> !dismissals.containsKey(popup.getId()) || !dismissals.get(popup.getId()).hides(popup, now))
            .toList();
        List<Long> fileIds = popups.stream().filter(AnnouncementPopup::hasImage)
            .map(AnnouncementPopup::getImageFileId).distinct().toList();
        Map<Long, String> imageUrls = fileIds.isEmpty() ? Map.of() : fileObjectRepository.findAllById(fileIds).stream()
            .collect(Collectors.toMap(FileObject::getId, file -> fileStorage.presignedUrl(file.getStorageKey())));
        return new StudentAnnouncementPopupListResponse(popups.stream()
            .map(popup -> AnnouncementPopupResponse.from(popup,
                popup.hasImage() ? imageUrls.get(popup.getImageFileId()) : null))
            .toList());
    }

    @Transactional
    public AnnouncementPopupDismissalResponse dismiss(Long popupId, String userId, long version) {
        LocalDateTime now = LocalDateTime.now(serviceClock);
        AnnouncementPopup popup = popupRepository.findAvailable(userId, popupId, now).stream().findFirst()
            .orElseThrow(AnnouncementPopupNotFoundException::new);
        if (popup.getVersion() != version) {
            throw new AnnouncementPopupConcurrentlyModifiedException();
        }
        AnnouncementPopupDismissal saved = dismissalRepository.save(
            AnnouncementPopupDismissal.create(userId, popup, now), now);
        return new AnnouncementPopupDismissalResponse(saved.popupId(), saved.popupVersion(), saved.hiddenUntil());
    }
}
