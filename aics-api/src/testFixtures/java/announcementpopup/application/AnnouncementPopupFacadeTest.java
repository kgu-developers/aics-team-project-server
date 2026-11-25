package announcementpopup.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import kgu.developers.api.announcementpopup.application.AnnouncementPopupFacade;
import kgu.developers.api.announcementpopup.presentation.request.AnnouncementPopupCreateRequest;
import kgu.developers.api.announcementpopup.presentation.request.AnnouncementPopupUpdateRequest;
import kgu.developers.api.announcementpopup.presentation.response.AnnouncementPopupListResponse;
import kgu.developers.api.announcementpopup.presentation.response.AnnouncementPopupResponse;
import kgu.developers.domain.announcementpopup.application.command.AnnouncementPopupCommandService;
import kgu.developers.domain.announcementpopup.application.query.AnnouncementPopupQueryService;
import kgu.developers.domain.announcementpopup.exception.AnnouncementPopupAlreadyExistsException;
import kgu.developers.domain.section.domain.Section;
import kgu.developers.domain.sectionannouncement.application.query.SectionAnnouncementQueryService;
import kgu.developers.domain.sectionannouncement.domain.SectionAnnouncement;
import kgu.developers.domain.sectionannouncement.exception.SectionAnnouncementNotFoundException;
import mock.repository.FakeAnnouncementPopupRepository;
import mock.repository.FakeFileObjectRepository;
import mock.repository.FakeFileStorage;
import mock.repository.FakeSectionAnnouncementRepository;
import mock.repository.FakeSectionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.multipart.MultipartFile;

public class AnnouncementPopupFacadeTest {

    private static final String PROFESSOR = "202400001";
    private static final String OTHER_PROFESSOR = "202400002";
    private static final String STUDENT = "202412345";

    private AnnouncementPopupFacade facade;
    private FakeAnnouncementPopupRepository fakePopupRepository;
    private FakeSectionAnnouncementRepository fakeAnnouncementRepository;
    private Long sectionId;
    private Long otherSectionId;

    @BeforeEach
    public void init() {
        FakeSectionRepository fakeSectionRepository = new FakeSectionRepository();
        fakePopupRepository = new FakeAnnouncementPopupRepository();
        fakeAnnouncementRepository = new FakeSectionAnnouncementRepository();
        FakeFileObjectRepository fakeFileObjectRepository = new FakeFileObjectRepository();
        FakeFileStorage fakeFileStorage = new FakeFileStorage();

        sectionId = fakeSectionRepository.save(
            Section.create(PROFESSOR, 1L, "DD015_1206", "월 1교시", 40, null, null)).getId();
        otherSectionId = fakeSectionRepository.save(
            Section.create(OTHER_PROFESSOR, 1L, "DD015_1207", "화 1교시", 40, null, null)).getId();

        facade = new AnnouncementPopupFacade(
            new AnnouncementPopupCommandService(fakePopupRepository, fakeFileObjectRepository, fakeFileStorage),
            new AnnouncementPopupQueryService(fakePopupRepository),
            new SectionAnnouncementQueryService(fakeAnnouncementRepository),
            fakeFileObjectRepository,
            fakeFileStorage,
            fakeSectionRepository
        );
    }

    private Long saveAnnouncement(Long targetSectionId) {
        return fakeAnnouncementRepository.save(
            SectionAnnouncement.create(targetSectionId, "공지 제목", "공지 내용", LocalDateTime.now())).getId();
    }

    private AnnouncementPopupCreateRequest createRequest(String content) {
        return AnnouncementPopupCreateRequest.builder().content(content).build();
    }

    private MultipartFile image() {
        return new MockMultipartFile("image", "안내.png", "image/png", "bytes".getBytes());
    }

    // ---------------------------------------------------------------- 등록

    @Test
    @DisplayName("createPopup은 담당 교수가 등록하면 부모 공지와 공지의 분반을 담은 응답을 준다")
    public void createPopup_ByProfessor_ReturnsResponse() {
        // given
        Long announcementId = saveAnnouncement(sectionId);

        // when
        AnnouncementPopupResponse response = facade.createPopup(
            announcementId, PROFESSOR, createRequest("안내 내용"), null);

        // then
        assertThat(response.id()).isNotNull();
        assertThat(response.announcementId()).isEqualTo(announcementId);
        assertThat(response.sectionId()).isEqualTo(sectionId);
        assertThat(response.content()).isEqualTo("안내 내용");
        assertThat(response.enabled()).isTrue();
    }

    @Test
    @DisplayName("createPopup은 이미지가 없으면 imageUrl이 null이다")
    public void createPopup_WithoutImage_ImageUrlIsNull() {
        // given
        Long announcementId = saveAnnouncement(sectionId);

        // when
        AnnouncementPopupResponse response = facade.createPopup(
            announcementId, PROFESSOR, createRequest("안내 내용"), null);

        // then
        assertThat(response.imageUrl()).isNull();
    }

    @Test
    @DisplayName("createPopup은 이미지를 보내면 응답에 임시 URL을 담는다")
    public void createPopup_WithImage_ReturnsImageUrl() {
        // given
        Long announcementId = saveAnnouncement(sectionId);

        // when
        AnnouncementPopupResponse response = facade.createPopup(
            announcementId, PROFESSOR, createRequest("안내 내용"), image());

        // then
        assertThat(response.imageUrl()).startsWith("https://fake-storage.local/");
    }

    @Test
    @DisplayName("createPopup은 담당 교수가 아니면 거부한다(다른 분반 교수, 학생 모두)")
    public void createPopup_ByNonProfessor_Denied() {
        // given
        Long announcementId = saveAnnouncement(sectionId);

        // when & then
        assertThatThrownBy(() -> facade.createPopup(announcementId, OTHER_PROFESSOR, createRequest("내용"), null))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> facade.createPopup(announcementId, STUDENT, createRequest("내용"), null))
            .isInstanceOf(AccessDeniedException.class);
        assertThat(fakePopupRepository.existsByAnnouncementId(announcementId)).isFalse();
    }

    @Test
    @DisplayName("createPopup은 없는 공지에 붙이면 NotFound를 던진다")
    public void createPopup_UnknownAnnouncement_NotFound() {
        assertThatThrownBy(() -> facade.createPopup(999L, PROFESSOR, createRequest("내용"), null))
            .isInstanceOf(SectionAnnouncementNotFoundException.class);
    }

    @Test
    @DisplayName("createPopup은 같은 공지에 두 번 등록하면 거부한다")
    public void createPopup_Duplicate_Rejected() {
        // given
        Long announcementId = saveAnnouncement(sectionId);
        facade.createPopup(announcementId, PROFESSOR, createRequest("첫 번째"), null);

        // when & then
        assertThatThrownBy(() -> facade.createPopup(announcementId, PROFESSOR, createRequest("두 번째"), null))
            .isInstanceOf(AnnouncementPopupAlreadyExistsException.class);
    }

    @Test
    @DisplayName("createPopup은 제목을 보내면 응답에 담고, 보내지 않으면 null이다")
    public void createPopup_Title_Optional() {
        // when
        AnnouncementPopupResponse withTitle = facade.createPopup(
            saveAnnouncement(sectionId), PROFESSOR,
            AnnouncementPopupCreateRequest.builder().title("중간보고서 안내").content("내용").build(), null);
        AnnouncementPopupResponse withoutTitle = facade.createPopup(
            saveAnnouncement(sectionId), PROFESSOR, createRequest("내용"), null);

        // then
        assertThat(withTitle.title()).isEqualTo("중간보고서 안내");
        assertThat(withoutTitle.title()).isNull();
    }

    // ---------------------------------------------------------------- 수정

    @Test
    @DisplayName("updatePopup은 제목을 바꾸고, 빈 문자열을 보내면 지운다")
    public void updatePopup_Title_ChangeAndClear() {
        // given
        Long popupId = facade.createPopup(saveAnnouncement(sectionId), PROFESSOR, createRequest("내용"), null).id();

        // when
        AnnouncementPopupResponse changed = facade.updatePopup(
            popupId, PROFESSOR, AnnouncementPopupUpdateRequest.builder().title("새 제목").build(), null);
        AnnouncementPopupResponse cleared = facade.updatePopup(
            popupId, PROFESSOR, AnnouncementPopupUpdateRequest.builder().title("").build(), null);

        // then
        assertThat(changed.title()).isEqualTo("새 제목");
        assertThat(cleared.title()).isNull();
    }

    @Test
    @DisplayName("updatePopup은 담당 교수가 내용과 노출 여부를 바꾸면 응답에 반영된다")
    public void updatePopup_ByProfessor_Reflected() {
        // given
        Long popupId = facade.createPopup(saveAnnouncement(sectionId), PROFESSOR, createRequest("원래 내용"), null).id();
        AnnouncementPopupUpdateRequest request = AnnouncementPopupUpdateRequest.builder()
            .content("수정된 내용").enabled(false).build();

        // when
        AnnouncementPopupResponse response = facade.updatePopup(popupId, PROFESSOR, request, null);

        // then
        assertThat(response.content()).isEqualTo("수정된 내용");
        assertThat(response.enabled()).isFalse();
    }

    @Test
    @DisplayName("updatePopup은 removeImage를 보내면 응답의 imageUrl이 null이 된다")
    public void updatePopup_RemoveImage_ImageUrlIsNull() {
        // given
        Long popupId = facade.createPopup(
            saveAnnouncement(sectionId), PROFESSOR, createRequest("내용"), image()).id();
        AnnouncementPopupUpdateRequest request = AnnouncementPopupUpdateRequest.builder().removeImage(true).build();

        // when
        AnnouncementPopupResponse response = facade.updatePopup(popupId, PROFESSOR, request, null);

        // then
        assertThat(response.imageUrl()).isNull();
    }

    @Test
    @DisplayName("updatePopup은 다른 분반 교수가 수정하려 하면 거부하고 내용을 바꾸지 않는다")
    public void updatePopup_ByOtherProfessor_DeniedAndUnchanged() {
        // given
        Long popupId = facade.createPopup(saveAnnouncement(sectionId), PROFESSOR, createRequest("원래 내용"), null).id();
        AnnouncementPopupUpdateRequest request = AnnouncementPopupUpdateRequest.builder().content("탈취").build();

        // when & then
        assertThatThrownBy(() -> facade.updatePopup(popupId, OTHER_PROFESSOR, request, null))
            .isInstanceOf(AccessDeniedException.class);
        assertThat(fakePopupRepository.findById(popupId).orElseThrow().getContent()).isEqualTo("원래 내용");
    }

    // ---------------------------------------------------------------- 목록

    @Test
    @DisplayName("getPopups는 해당 분반의 팝업만 최신순으로, 꺼진 팝업도 포함해 반환한다")
    public void getPopups_OwnSectionNewestFirstIncludingDisabled() {
        // given
        Long firstId = facade.createPopup(saveAnnouncement(sectionId), PROFESSOR, createRequest("첫 번째"), null).id();
        Long secondId = facade.createPopup(saveAnnouncement(sectionId), PROFESSOR, createRequest("두 번째"), null).id();
        facade.createPopup(saveAnnouncement(otherSectionId), OTHER_PROFESSOR, createRequest("다른 분반"), null);
        facade.updatePopup(firstId, PROFESSOR, AnnouncementPopupUpdateRequest.builder().enabled(false).build(), null);

        // when
        AnnouncementPopupListResponse response = facade.getPopups(sectionId, PROFESSOR);

        // then
        assertThat(response.contents()).extracting(AnnouncementPopupResponse::id)
            .containsExactly(secondId, firstId);
        assertThat(response.contents().get(1).enabled()).isFalse();
    }

    @Test
    @DisplayName("getPopups는 이미지가 있는 팝업과 없는 팝업이 섞여 있어도 각자 올바른 imageUrl을 준다")
    public void getPopups_MixedImages() {
        // given
        facade.createPopup(saveAnnouncement(sectionId), PROFESSOR, createRequest("이미지 있음"), image());
        facade.createPopup(saveAnnouncement(sectionId), PROFESSOR, createRequest("이미지 없음"), null);

        // when
        AnnouncementPopupListResponse response = facade.getPopups(sectionId, PROFESSOR);

        // then (최신순: 이미지 없음 → 이미지 있음)
        assertThat(response.contents().get(0).imageUrl()).isNull();
        assertThat(response.contents().get(1).imageUrl()).startsWith("https://fake-storage.local/");
    }

    @Test
    @DisplayName("getPopups는 팝업이 모두 이미지 없이 등록돼 있어도 오류 없이 반환한다")
    public void getPopups_NoImagesAtAll() {
        // given
        facade.createPopup(saveAnnouncement(sectionId), PROFESSOR, createRequest("내용"), null);

        // when
        AnnouncementPopupListResponse response = facade.getPopups(sectionId, PROFESSOR);

        // then
        assertThat(response.contents()).hasSize(1);
        assertThat(response.contents().get(0).imageUrl()).isNull();
    }

    @Test
    @DisplayName("getPopups는 담당 교수가 아니면 거부한다")
    public void getPopups_ByNonProfessor_Denied() {
        assertThatThrownBy(() -> facade.getPopups(sectionId, STUDENT))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> facade.getPopups(sectionId, OTHER_PROFESSOR))
            .isInstanceOf(AccessDeniedException.class);
    }
}
