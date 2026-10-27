package kgu.developers.api.sectionannouncement.application;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import kgu.developers.api.sectionannouncement.presentation.request.SectionAnnouncementCreateRequest;
import kgu.developers.api.sectionannouncement.presentation.request.SectionAnnouncementUpdateRequest;
import kgu.developers.api.sectionannouncement.presentation.response.SectionAnnouncementAttachmentResponse;
import kgu.developers.api.sectionannouncement.presentation.response.SectionAnnouncementListResponse;
import kgu.developers.api.sectionannouncement.presentation.response.SectionAnnouncementResponse;
import kgu.developers.domain.enrollment.domain.Enrollment;
import kgu.developers.domain.enrollment.domain.EnrollmentRepository;
import kgu.developers.domain.enrollment.domain.Status;
import kgu.developers.domain.fileobject.domain.FileObject;
import kgu.developers.domain.fileobject.domain.FileObjectRepository;
import kgu.developers.domain.fileobject.domain.FileStorage;
import kgu.developers.domain.notification.application.command.NotificationCommandService;
import kgu.developers.domain.notification.domain.NotificationType;
import kgu.developers.domain.section.domain.SectionRepository;
import kgu.developers.domain.sectionannouncement.application.command.SectionAnnouncementCommandService;
import kgu.developers.domain.sectionannouncement.application.query.SectionAnnouncementQueryService;
import kgu.developers.domain.sectionannouncement.domain.SectionAnnouncement;
import kgu.developers.domain.sectionannouncement.domain.SectionAnnouncementAttachment;
import kgu.developers.domain.sectionannouncement.domain.SectionAnnouncementAttachmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Component
@Transactional
@RequiredArgsConstructor
public class SectionAnnouncementFacade {

    private final SectionAnnouncementCommandService sectionAnnouncementCommandService;
    private final SectionAnnouncementQueryService sectionAnnouncementQueryService;
    private final SectionAnnouncementAttachmentRepository sectionAnnouncementAttachmentRepository;
    private final FileObjectRepository fileObjectRepository;
    private final FileStorage fileStorage;
    private final NotificationCommandService notificationCommandService;
    private final SectionRepository sectionRepository;
    private final EnrollmentRepository enrollmentRepository;

    public SectionAnnouncementListResponse getAnnouncements(Long sectionId, String userId) {
        validateSectionMember(sectionId, userId);
        List<SectionAnnouncement> announcements = sectionAnnouncementQueryService.getAnnouncements(sectionId);
        List<Long> announcementIds = announcements.stream().map(SectionAnnouncement::getId).toList();
        Map<Long, List<SectionAnnouncementAttachmentResponse>> attachmentsByAnnouncementId =
            resolveAttachments(sectionAnnouncementAttachmentRepository.findAllByAnnouncementIdIn(announcementIds));
        return SectionAnnouncementListResponse.from(announcements, attachmentsByAnnouncementId);
    }

    public SectionAnnouncementResponse createAnnouncement(
        Long sectionId, String userId, SectionAnnouncementCreateRequest request, List<MultipartFile> files
    ) {
        validateProfessor(sectionId, userId);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime publishedAt = request.publishedAt() != null ? request.publishedAt() : now;
        Long id = sectionAnnouncementCommandService.createAnnouncement(
            sectionId, request.title(), request.content(), publishedAt, files, userId);

        // 예약 게시(publishedAt이 미래)면 알림은 지금 보내지 않는다 — publishScheduledAnnouncements()가 게시 시각에 대신 발송한다.
        if (!publishedAt.isAfter(now)) {
            broadcastToSection(sectionId, id, request.title());
            sectionAnnouncementCommandService.markNotified(id, now);
        }

        return buildResponse(id);
    }

    // 예약 게시 공지 중 게시 시각이 지났지만 아직 알림을 못 받은 것들을 발송한다. SectionAnnouncementNotificationScheduler가 주기 호출.
    public void publishScheduledAnnouncements() {
        LocalDateTime now = LocalDateTime.now();
        sectionAnnouncementQueryService.getAnnouncementsToNotify(now).forEach(announcement -> {
            broadcastToSection(announcement.getSectionId(), announcement.getId(), announcement.getTitle());
            sectionAnnouncementCommandService.markNotified(announcement.getId(), now);
        });
    }

    public SectionAnnouncementResponse updateAnnouncement(
        Long id, String userId, SectionAnnouncementUpdateRequest request, List<MultipartFile> files
    ) {
        SectionAnnouncement announcement = sectionAnnouncementQueryService.getAnnouncement(id);
        validateProfessor(announcement.getSectionId(), userId);

        sectionAnnouncementCommandService.updateAnnouncement(
            id, request.title(), request.content(), request.publishedAt(),
            files, request.deleteAttachmentIds(), userId);
        return buildResponse(id);
    }

    private SectionAnnouncementResponse buildResponse(Long id) {
        SectionAnnouncement announcement = sectionAnnouncementQueryService.getAnnouncement(id);
        Map<Long, List<SectionAnnouncementAttachmentResponse>> attachmentsByAnnouncementId =
            resolveAttachments(sectionAnnouncementAttachmentRepository.findAllByAnnouncementId(id));
        return SectionAnnouncementResponse.from(announcement, attachmentsByAnnouncementId.getOrDefault(id, List.of()));
    }

    private Map<Long, List<SectionAnnouncementAttachmentResponse>> resolveAttachments(
        List<SectionAnnouncementAttachment> attachments
    ) {
        if (attachments.isEmpty()) {
            return Map.of();
        }
        List<Long> fileIds = attachments.stream().map(SectionAnnouncementAttachment::getFileId).distinct().toList();
        Map<Long, FileObject> fileObjectsById = fileObjectRepository.findAllById(fileIds).stream()
            .collect(Collectors.toMap(FileObject::getId, Function.identity()));

        return attachments.stream()
            .collect(Collectors.groupingBy(
                SectionAnnouncementAttachment::getAnnouncementId,
                Collectors.mapping(attachment -> {
                    FileObject fileObject = fileObjectsById.get(attachment.getFileId());
                    return SectionAnnouncementAttachmentResponse.of(
                        attachment, fileObject, fileStorage.presignedUrl(fileObject.getStorageKey()));
                }, Collectors.toList())
            ));
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
