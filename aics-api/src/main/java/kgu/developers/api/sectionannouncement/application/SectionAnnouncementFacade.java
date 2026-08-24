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

        LocalDateTime publishedAt = request.publishedAt() != null ? request.publishedAt() : LocalDateTime.now();
        Long id = sectionAnnouncementCommandService.createAnnouncement(sectionId, request.title(), request.content(), publishedAt);

        broadcastToSection(sectionId, request.title());

        return SectionAnnouncementResponse.from(sectionAnnouncementQueryService.getAnnouncement(id));
    }

    public SectionAnnouncementResponse updateAnnouncement(Long id, String userId, SectionAnnouncementUpdateRequest request) {
        SectionAnnouncement announcement = sectionAnnouncementQueryService.getAnnouncement(id);
        validateProfessor(announcement.getSectionId(), userId);

        sectionAnnouncementCommandService.updateAnnouncement(id, request.title(), request.content(), request.publishedAt());
        return SectionAnnouncementResponse.from(sectionAnnouncementQueryService.getAnnouncement(id));
    }

    // 등록 성공 시 분반 소속(ACTIVE) 전원에게 브로드캐스트한다(교수 요구사항).
    private void broadcastToSection(Long sectionId, String title) {
        List<String> targetUserIds = enrollmentRepository.findAllBySectionId(sectionId).stream()
            .filter(enrollment -> enrollment.getStatus() == Status.ACTIVE)
            .map(Enrollment::getUserId)
            .toList();

        notificationCommandService.broadcast(
            targetUserIds,
            NotificationType.SECTION_ANNOUNCEMENT,
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
