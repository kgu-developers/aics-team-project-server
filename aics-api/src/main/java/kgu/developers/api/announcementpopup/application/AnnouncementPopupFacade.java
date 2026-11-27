package kgu.developers.api.announcementpopup.application;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import kgu.developers.api.announcementpopup.presentation.request.AnnouncementPopupCreateRequest;
import kgu.developers.api.announcementpopup.presentation.request.AnnouncementPopupUpdateRequest;
import kgu.developers.api.announcementpopup.presentation.response.AnnouncementPopupListResponse;
import kgu.developers.api.announcementpopup.presentation.response.AnnouncementPopupResponse;
import kgu.developers.domain.announcementpopup.application.command.AnnouncementPopupCommandService;
import kgu.developers.domain.announcementpopup.application.query.AnnouncementPopupQueryService;
import kgu.developers.domain.announcementpopup.domain.AnnouncementPopup;
import kgu.developers.domain.fileobject.domain.FileObject;
import kgu.developers.domain.fileobject.domain.FileObjectRepository;
import kgu.developers.domain.fileobject.domain.FileStorage;
import kgu.developers.domain.section.domain.SectionRepository;
import kgu.developers.domain.sectionannouncement.application.query.SectionAnnouncementQueryService;
import kgu.developers.domain.sectionannouncement.domain.SectionAnnouncement;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Component
@Transactional
@RequiredArgsConstructor
public class AnnouncementPopupFacade {

    private final AnnouncementPopupCommandService announcementPopupCommandService;
    private final AnnouncementPopupQueryService announcementPopupQueryService;
    private final SectionAnnouncementQueryService sectionAnnouncementQueryService;
    private final FileObjectRepository fileObjectRepository;
    private final FileStorage fileStorage;
    private final SectionRepository sectionRepository;

    public AnnouncementPopupResponse createPopup(
        Long announcementId, String userId, AnnouncementPopupCreateRequest request, MultipartFile image
    ) {
        // 팝업의 분반은 요청이 아니라 부모 공지에서 가져온다. 다른 분반 공지에 팝업을 붙이는 일을 구조적으로 막는다.
        SectionAnnouncement announcement = sectionAnnouncementQueryService.getAnnouncement(announcementId);
        validateProfessor(announcement.getSectionId(), userId);

        Long id = announcementPopupCommandService.createPopup(
            announcementId, announcement.getSectionId(), request.title(), request.content(), image, userId);
        return buildResponse(announcementPopupQueryService.getPopup(id));
    }

    public AnnouncementPopupResponse updatePopup(
        Long id, String userId, AnnouncementPopupUpdateRequest request, MultipartFile image
    ) {
        AnnouncementPopup popup = announcementPopupQueryService.getPopup(id);
        validateProfessor(popup.getSectionId(), userId);

        announcementPopupCommandService.updatePopup(
            id, request.title(), request.content(), request.enabled(),
            Boolean.TRUE.equals(request.removeImage()), image, userId);
        return buildResponse(announcementPopupQueryService.getPopup(id));
    }

    public AnnouncementPopupListResponse getPopups(Long sectionId, String userId) {
        validateProfessor(sectionId, userId);

        List<AnnouncementPopup> popups = announcementPopupQueryService.getPopups(sectionId);
        Map<Long, String> imageUrlsByFileId = resolveImageUrls(popups);
        return AnnouncementPopupListResponse.from(popups.stream()
            .map(popup -> AnnouncementPopupResponse.from(popup, imageUrlOf(popup, imageUrlsByFileId)))
            .toList());
    }

    private AnnouncementPopupResponse buildResponse(AnnouncementPopup popup) {
        return AnnouncementPopupResponse.from(popup, imageUrlOf(popup, resolveImageUrls(List.of(popup))));
    }

    // 이미지가 없는 팝업의 imageFileId는 null이라, 불변 Map(Map.of)에서 get(null)을 하면 NPE가 난다.
    // 그래서 이미지가 있을 때만 조회한다.
    private String imageUrlOf(AnnouncementPopup popup, Map<Long, String> imageUrlsByFileId) {
        return popup.hasImage() ? imageUrlsByFileId.get(popup.getImageFileId()) : null;
    }

    private Map<Long, String> resolveImageUrls(List<AnnouncementPopup> popups) {
        List<Long> fileIds = popups.stream()
            .filter(AnnouncementPopup::hasImage)
            .map(AnnouncementPopup::getImageFileId)
            .distinct()
            .toList();
        if (fileIds.isEmpty()) {
            return Map.of();
        }
        return fileObjectRepository.findAllById(fileIds).stream()
            .collect(Collectors.toMap(
                FileObject::getId,
                fileObject -> fileStorage.presignedUrl(fileObject.getStorageKey()),
                (first, second) -> first));
    }

    private void validateProfessor(Long sectionId, String userId) {
        if (!sectionRepository.existsActiveByIdAndProfessorId(sectionId, userId)) {
            throw new AccessDeniedException("담당 교수만 팝업을 등록/수정/조회할 수 있습니다.");
        }
    }
}
