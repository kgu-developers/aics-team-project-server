package kgu.developers.domain.announcementpopup.infrastructure;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaAnnouncementPopupDismissalRepository extends JpaRepository<AnnouncementPopupDismissalJpaEntity, Long> {
    List<AnnouncementPopupDismissalJpaEntity> findAllByUserId(String userId);

    // 원자적 upsert로 최초 동시 요청의 유일 제약 충돌을 막는다. 늦게 도착한 이전 버전/시각은 되돌리지 않는다.
    @Query(value = """
        INSERT INTO announcement_popup_dismissal
            (user_id, popup_id, popup_version, hidden_until, created_at, updated_at)
        VALUES (:userId, :popupId, :popupVersion, :hiddenUntil, :now, :now)
        ON CONFLICT (user_id, popup_id) DO UPDATE SET
            popup_version = GREATEST(announcement_popup_dismissal.popup_version, EXCLUDED.popup_version),
            hidden_until = CASE
                WHEN EXCLUDED.popup_version > announcement_popup_dismissal.popup_version THEN EXCLUDED.hidden_until
                WHEN EXCLUDED.popup_version = announcement_popup_dismissal.popup_version
                    THEN GREATEST(announcement_popup_dismissal.hidden_until, EXCLUDED.hidden_until)
                ELSE announcement_popup_dismissal.hidden_until END,
            updated_at = GREATEST(announcement_popup_dismissal.updated_at, EXCLUDED.updated_at)
        RETURNING *
        """, nativeQuery = true)
    AnnouncementPopupDismissalJpaEntity upsert(
        @Param("userId") String userId, @Param("popupId") Long popupId,
        @Param("popupVersion") long popupVersion, @Param("hiddenUntil") LocalDateTime hiddenUntil,
        @Param("now") LocalDateTime now);
}
