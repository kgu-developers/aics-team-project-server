package kgu.developers.api.sectionannouncement.application;

import java.time.LocalDateTime;
import java.util.List;
import kgu.developers.api.sectionannouncement.presentation.request.SectionAnnouncementCreateRequest;
import kgu.developers.api.sectionannouncement.presentation.request.SectionAnnouncementUpdateRequest;
import kgu.developers.api.sectionannouncement.presentation.response.SectionAnnouncementListResponse;
import kgu.developers.api.sectionannouncement.presentation.response.SectionAnnouncementResponse;
import kgu.developers.domain.enrollment.domain.Enrollment;
import kgu.developers.domain.enrollment.domain.EnrollmentRepository;
import kgu.developers.domain.enrollment.domain.Status;
import kgu.developers.domain.notification.application.command.NotificationCommandService;
import kgu.developers.domain.notification.domain.NotificationType;
import kgu.developers.domain.section.domain.SectionRepository;
import kgu.developers.domain.sectionannouncement.application.command.SectionAnnouncementCommandService;
import kgu.developers.domain.sectionannouncement.application.query.SectionAnnouncementQueryService;
import kgu.developers.domain.sectionannouncement.domain.SectionAnnouncement;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
@RequiredArgsConstructor
public class SectionAnnouncementFacade {

    private final SectionAnnouncementCommandService sectionAnnouncementCommandService;
    private final SectionAnnouncementQueryService sectionAnnouncementQueryService;
    private final NotificationCommandService notificationCommandService;
    private final SectionRepository sectionRepository;
    private final EnrollmentRepository enrollmentRepository;

    public SectionAnnouncementListResponse getAnnouncements(Long sectionId, String userId) {
        validateSectionMember(sectionId, userId);
        return SectionAnnouncementListResponse.from(sectionAnnouncementQueryService.getAnnouncements(sectionId));
    }

    public SectionAnnouncementResponse createAnnouncement(Long sectionId, String userId, SectionAnnouncementCreateRequest request) {
        validateProfessor(sectionId, userId);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime publishedAt = request.publishedAt() != null ? request.publishedAt() : now;
        Long id = sectionAnnouncementCommandService.createAnnouncement(sectionId, request.title(), request.content(), publishedAt);

        // 예약 게시(publishedAt이 미래)면 알림은 지금 보내지 않는다 — publishScheduledAnnouncements()가 게시 시각에 대신 발송한다.
        if (!publishedAt.isAfter(now)) {
            broadcastToSection(sectionId, id, request.title());
            sectionAnnouncementCommandService.markNotified(id, now);
        }

        return SectionAnnouncementResponse.from(sectionAnnouncementQueryService.getAnnouncement(id));
    }

    // 예약 게시 공지 중 게시 시각이 지났지만 아직 알림을 못 받은 것들을 발송한다. SectionAnnouncementNotificationScheduler가 주기 호출.
    public void publishScheduledAnnouncements() {
        LocalDateTime now = LocalDateTime.now();
        sectionAnnouncementQueryService.getAnnouncementsToNotify(now).forEach(announcement -> {
            broadcastToSection(announcement.getSectionId(), announcement.getId(), announcement.getTitle());
            sectionAnnouncementCommandService.markNotified(announcement.getId(), now);
        });
    }

    public SectionAnnouncementResponse updateAnnouncement(Long id, String userId, SectionAnnouncementUpdateRequest request) {
        SectionAnnouncement announcement = sectionAnnouncementQueryService.getAnnouncement(id);
        validateProfessor(announcement.getSectionId(), userId);

        sectionAnnouncementCommandService.updateAnnouncement(id, request.title(), request.content(), request.publishedAt());
        return SectionAnnouncementResponse.from(sectionAnnouncementQueryService.getAnnouncement(id));
    }

    // 등록 성공 시 분반 소속(ACTIVE) 전원에게 브로드캐스트한다(교수 요구사항).
    private void broadcastToSection(Long sectionId, Long announcementId, String title) {
        List<String> targetUserIds = enrollmentRepository.findAllBySectionId(sectionId).stream()
            .filter(enrollment -> enrollment.getStatus() == Status.ACTIVE)
            .map(Enrollment::getUserId)
            .toList();

        notificationCommandService.broadcast(
            targetUserIds,
            NotificationType.SECTION_ANNOUNCEMENT,
            announcementId,
            "새 공지사항: " + title,
            title,
            null
        );
    }

    private void validateSectionMember(Long sectionId, String userId) {
        boolean isProfessor = sectionRepository.existsActiveByIdAndProfessorId(sectionId, userId);
        boolean isEnrolled = enrollmentRepository.findBySectionIdAndUserId(sectionId, userId)
            .filter(enrollment -> enrollment.getStatus() == Status.ACTIVE)
            .isPresent();
        if (!isProfessor && !isEnrolled) {
            throw new AccessDeniedException("해당 분반에 소속된 사용자만 공지사항에 접근할 수 있습니다.");
        }
    }

    private void validateProfessor(Long sectionId, String userId) {
        if (!sectionRepository.existsActiveByIdAndProfessorId(sectionId, userId)) {
            throw new AccessDeniedException("담당 교수만 공지사항을 등록/수정할 수 있습니다.");
        }
    }
}
