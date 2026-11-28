package kgu.developers.domain.announcementpopup.domain;

import java.time.LocalDateTime;

public record AnnouncementPopupDismissal(
    String userId, Long popupId, long contentVersion, LocalDateTime hiddenUntil
) {
    public static AnnouncementPopupDismissal create(String userId, AnnouncementPopup popup, LocalDateTime now) {
        return new AnnouncementPopupDismissal(userId, popup.getId(), popup.getContentVersion(), now.plusDays(7));
    }

    public boolean hides(AnnouncementPopup popup, LocalDateTime now) {
        return popupId.equals(popup.getId()) && contentVersion == popup.getContentVersion() && now.isBefore(hiddenUntil);
    }
}
