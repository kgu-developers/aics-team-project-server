package kgu.developers.domain.announcementpopup.domain;

import java.time.LocalDateTime;

public record AnnouncementPopupDismissal(
    String userId, Long popupId, long popupVersion, LocalDateTime hiddenUntil
) {
    public static AnnouncementPopupDismissal create(String userId, AnnouncementPopup popup, LocalDateTime now) {
        return new AnnouncementPopupDismissal(userId, popup.getId(), popup.getVersion(), now.plusDays(7));
    }

    public boolean hides(AnnouncementPopup popup, LocalDateTime now) {
        return popupId.equals(popup.getId()) && popupVersion == popup.getVersion() && now.isBefore(hiddenUntil);
    }
}
