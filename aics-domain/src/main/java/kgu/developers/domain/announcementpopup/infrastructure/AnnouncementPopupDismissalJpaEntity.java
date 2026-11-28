package kgu.developers.domain.announcementpopup.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import kgu.developers.common.domain.BaseTimeEntity;
import kgu.developers.domain.announcementpopup.domain.AnnouncementPopupDismissal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "announcement_popup_dismissal", uniqueConstraints = @UniqueConstraint(
    name = "uk_popup_dismissal_user_popup", columnNames = {"user_id", "popup_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AnnouncementPopupDismissalJpaEntity extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, length = 20)
    private String userId;

    @Column(name = "popup_id", nullable = false)
    private Long popupId;

    // 기존 컬럼명이며, 낙관적 락 version이 아닌 내용 변경용 contentVersion을 저장한다.
    @Column(name = "popup_version", nullable = false)
    private long contentVersion;

    @Column(name = "hidden_until", nullable = false)
    private LocalDateTime hiddenUntil;

    public AnnouncementPopupDismissal toDomain() {
        return new AnnouncementPopupDismissal(userId, popupId, contentVersion, hiddenUntil);
    }
}
