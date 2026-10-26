package kgu.developers.domain.sectionannouncement.application.command;

import java.time.LocalDateTime;
import java.util.List;
import kgu.developers.domain.fileobject.domain.FileObject;
import kgu.developers.domain.fileobject.domain.FileObjectRepository;
import kgu.developers.domain.fileobject.domain.FileStorage;
import kgu.developers.domain.sectionannouncement.domain.SectionAnnouncement;
import kgu.developers.domain.sectionannouncement.domain.SectionAnnouncementAttachment;
import kgu.developers.domain.sectionannouncement.domain.SectionAnnouncementAttachmentRepository;
import kgu.developers.domain.sectionannouncement.domain.SectionAnnouncementRepository;
import kgu.developers.domain.sectionannouncement.exception.SectionAnnouncementAttachmentNotFoundException;
import kgu.developers.domain.sectionannouncement.exception.SectionAnnouncementEmptyUpdateException;
import kgu.developers.domain.sectionannouncement.exception.SectionAnnouncementInvalidContentException;
import kgu.developers.domain.sectionannouncement.exception.SectionAnnouncementNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Transactional
public class SectionAnnouncementCommandService {

    private final SectionAnnouncementRepository sectionAnnouncementRepository;
    private final SectionAnnouncementAttachmentRepository sectionAnnouncementAttachmentRepository;
    private final FileObjectRepository fileObjectRepository;
    private final FileStorage fileStorage;

    public Long createAnnouncement(
        Long sectionId, String title, String content, LocalDateTime publishedAt,
        List<MultipartFile> files, String uploaderId
    ) {
        SectionAnnouncement announcement = SectionAnnouncement.create(sectionId, title, content, publishedAt);
        Long id = sectionAnnouncementRepository.save(announcement).getId();
        attachFiles(id, files, uploaderId);
        return id;
    }

    public void updateAnnouncement(
        Long id, String title, String content, LocalDateTime publishedAt,
        List<MultipartFile> newFiles, List<Long> deleteAttachmentIds, String uploaderId
    ) {
        boolean hasFileChange = (newFiles != null && !newFiles.isEmpty())
            || (deleteAttachmentIds != null && !deleteAttachmentIds.isEmpty());
        if (title == null && content == null && publishedAt == null && !hasFileChange) {
            throw new SectionAnnouncementEmptyUpdateException();
        }

        SectionAnnouncement announcement = findOrThrow(id);

        if (title != null) {
            if (title.isBlank()) {
                throw new SectionAnnouncementInvalidContentException();
            }
            announcement.updateTitle(title);
        }
        if (content != null) {
            if (content.isBlank()) {
                throw new SectionAnnouncementInvalidContentException();
            }
            announcement.updateContent(content);
        }
        if (publishedAt != null) {
            announcement.updatePublishedAt(publishedAt);
        }

        sectionAnnouncementRepository.save(announcement);

        detachFiles(id, deleteAttachmentIds);
        attachFiles(id, newFiles, uploaderId);
    }

    public void markNotified(Long id, LocalDateTime notifiedAt) {
        SectionAnnouncement announcement = findOrThrow(id);
        announcement.markNotified(notifiedAt);
        sectionAnnouncementRepository.save(announcement);
    }

    private void attachFiles(Long announcementId, List<MultipartFile> files, String uploaderId) {
        if (files == null) {
            return;
        }
        for (MultipartFile file : files) {
            String storageKey = fileStorage.upload(file);
            FileObject fileObject = fileObjectRepository.save(FileObject.create(
                uploaderId, storageKey, file.getOriginalFilename(), file.getContentType(), file.getSize(), false, null));
            sectionAnnouncementAttachmentRepository.save(
                SectionAnnouncementAttachment.create(announcementId, fileObject.getId()));
        }
    }

    // 첨부 해제는 연결(join) 행만 지운다 — FileObject/S3 원본은 SubmissionArtifact 등 다른 기능과 동일하게
    // 보관한다(이 코드베이스엔 FileObject를 실제로 지우는 경로가 없음, fileobject 도메인 참고).
    private void detachFiles(Long announcementId, List<Long> attachmentIds) {
        if (attachmentIds == null) {
            return;
        }
        for (Long attachmentId : attachmentIds) {
            sectionAnnouncementAttachmentRepository.findByIdAndAnnouncementId(attachmentId, announcementId)
                .orElseThrow(SectionAnnouncementAttachmentNotFoundException::new);
            sectionAnnouncementAttachmentRepository.deleteById(attachmentId);
        }
    }

    private SectionAnnouncement findOrThrow(Long id) {
        return sectionAnnouncementRepository.findById(id)
            .orElseThrow(SectionAnnouncementNotFoundException::new);
    }
}
