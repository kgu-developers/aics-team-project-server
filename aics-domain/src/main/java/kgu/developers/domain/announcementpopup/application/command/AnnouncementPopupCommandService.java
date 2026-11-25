package kgu.developers.domain.announcementpopup.application.command;

import java.util.Set;
import kgu.developers.domain.announcementpopup.domain.AnnouncementPopup;
import kgu.developers.domain.announcementpopup.domain.AnnouncementPopupRepository;
import kgu.developers.domain.announcementpopup.exception.AnnouncementPopupAlreadyExistsException;
import kgu.developers.domain.announcementpopup.exception.AnnouncementPopupEmptyUpdateException;
import kgu.developers.domain.announcementpopup.exception.AnnouncementPopupImageConflictException;
import kgu.developers.domain.announcementpopup.exception.AnnouncementPopupInvalidContentException;
import kgu.developers.domain.announcementpopup.exception.AnnouncementPopupNotFoundException;
import kgu.developers.domain.announcementpopup.exception.AnnouncementPopupTitleTooLongException;
import kgu.developers.domain.fileobject.domain.FileObject;
import kgu.developers.domain.fileobject.domain.FileObjectRepository;
import kgu.developers.domain.fileobject.domain.FileStorage;
import kgu.developers.domain.fileobject.exception.FileObjectInvalidTypeException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Transactional
public class AnnouncementPopupCommandService {

    // file_object.file_name 컬럼 길이(FileObjectJpaEntity)와 맞춤 — 넘으면 DB insert가 실패해 S3엔
    // 이미 올라간 파일이 고아로 남으므로, 업로드 전에 여기서 먼저 잘라낸다.
    private static final int MAX_FILE_NAME_LENGTH = 255;
    // AnnouncementPopupJpaEntity.title 칼럼 길이와 맞춤. 넘으면 DB 무결성 오류가 나는데, 저장소는 그 오류를
    // "공지당 팝업 1개 유일 제약 위반"으로 해석하므로 여기서 먼저 걸러 잘못된 오류 응답을 막는다.
    private static final int MAX_TITLE_LENGTH = 200;
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
        "image/jpeg", "image/png", "image/gif", "image/webp");

    private final AnnouncementPopupRepository announcementPopupRepository;
    private final FileObjectRepository fileObjectRepository;
    private final FileStorage fileStorage;

    /** 공지당 팝업은 하나다. 이미 있으면 {@link AnnouncementPopupAlreadyExistsException}. */
    public Long createPopup(
        Long announcementId, Long sectionId, String title, String content, MultipartFile image, String uploaderId
    ) {
        validateTitle(title);
        validateContent(content);
        validateImage(image);
        if (announcementPopupRepository.existsByAnnouncementId(announcementId)) {
            throw new AnnouncementPopupAlreadyExistsException();
        }

        Long imageFileId = hasImage(image) ? uploadImage(image, uploaderId) : null;
        AnnouncementPopup popup = AnnouncementPopup.create(announcementId, sectionId, title, content, imageFileId);
        return announcementPopupRepository.save(popup).getId();
    }

    /**
     * 요청에 포함된 항목만 수정한다. 제목은 빈 문자열이나 공백을 보내면 지워진다(제목은 선택).
     * 이미지는 새 파일을 보내면 교체되고, {@code removeImage}가 true면 제거된다. 둘을 함께 보내면
     * 의도를 알 수 없으므로 거부한다.
     */
    public void updatePopup(
        Long id, String title, String content, Boolean enabled, boolean removeImage,
        MultipartFile image, String uploaderId
    ) {
        boolean hasNewImage = hasImage(image);
        if (title == null && content == null && enabled == null && !removeImage && !hasNewImage) {
            throw new AnnouncementPopupEmptyUpdateException();
        }
        if (removeImage && hasNewImage) {
            throw new AnnouncementPopupImageConflictException();
        }
        validateTitle(title);
        if (content != null) {
            validateContent(content);
        }
        validateImage(image);

        AnnouncementPopup popup = announcementPopupRepository.findById(id)
            .orElseThrow(AnnouncementPopupNotFoundException::new);

        if (title != null) {
            popup.updateTitle(title);
        }
        if (content != null) {
            popup.updateContent(content);
        }
        if (enabled != null) {
            popup.changeEnabled(enabled);
        }
        if (removeImage) {
            popup.removeImage();
        }
        if (hasNewImage) {
            popup.changeImage(uploadImage(image, uploaderId));
        }
        announcementPopupRepository.save(popup);
    }

    private void validateTitle(String title) {
        if (title != null && title.length() > MAX_TITLE_LENGTH) {
            throw new AnnouncementPopupTitleTooLongException();
        }
    }

    private void validateContent(String content) {
        if (content == null || content.isBlank()) {
            throw new AnnouncementPopupInvalidContentException();
        }
    }

    private void validateImage(MultipartFile image) {
        if (hasImage(image) && !ALLOWED_IMAGE_TYPES.contains(image.getContentType())) {
            throw new FileObjectInvalidTypeException();
        }
    }

    private boolean hasImage(MultipartFile image) {
        return image != null && !image.isEmpty();
    }

    private Long uploadImage(MultipartFile image, String uploaderId) {
        String fileName = sanitizeFileName(image.getOriginalFilename());
        String storageKey = fileStorage.upload(image, image.getContentType(), fileName);
        return fileObjectRepository.save(FileObject.create(
            uploaderId, storageKey, fileName, image.getContentType(), image.getSize(), false, null)).getId();
    }

    private String sanitizeFileName(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return "팝업이미지";
        }
        if (originalFilename.length() > MAX_FILE_NAME_LENGTH) {
            return originalFilename.substring(0, MAX_FILE_NAME_LENGTH);
        }
        return originalFilename;
    }
}
