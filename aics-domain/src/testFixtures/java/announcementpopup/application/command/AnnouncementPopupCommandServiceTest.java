package announcementpopup.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import kgu.developers.domain.announcementpopup.application.command.AnnouncementPopupCommandService;
import kgu.developers.domain.announcementpopup.domain.AnnouncementPopup;
import kgu.developers.domain.announcementpopup.exception.AnnouncementPopupAlreadyExistsException;
import kgu.developers.domain.announcementpopup.exception.AnnouncementPopupEmptyUpdateException;
import kgu.developers.domain.announcementpopup.exception.AnnouncementPopupImageConflictException;
import kgu.developers.domain.announcementpopup.exception.AnnouncementPopupInvalidContentException;
import kgu.developers.domain.announcementpopup.exception.AnnouncementPopupNotFoundException;
import kgu.developers.domain.announcementpopup.exception.AnnouncementPopupTitleTooLongException;
import kgu.developers.domain.fileobject.exception.FileObjectInvalidTypeException;
import mock.repository.FakeAnnouncementPopupRepository;
import mock.repository.FakeFileObjectRepository;
import mock.repository.FakeFileStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

class AnnouncementPopupCommandServiceTest {

    private static final String UPLOADER = "202400001";

    private FakeAnnouncementPopupRepository fakePopupRepository;
    private FakeFileObjectRepository fakeFileObjectRepository;
    private AnnouncementPopupCommandService commandService;

    @BeforeEach
    void init() {
        fakePopupRepository = new FakeAnnouncementPopupRepository();
        fakeFileObjectRepository = new FakeFileObjectRepository();
        commandService = new AnnouncementPopupCommandService(
            fakePopupRepository, fakeFileObjectRepository, new FakeFileStorage());
    }

    private MultipartFile image(String name, String contentType) {
        return new MockMultipartFile("image", name, contentType, "bytes".getBytes());
    }

    private Long createPopup() {
        return commandService.createPopup(10L, 1L, null, "안내 내용", null, UPLOADER);
    }

    private Long createPopupWithImage() {
        return commandService.createPopup(10L, 1L, null, "안내 내용", image("안내.png", "image/png"), UPLOADER);
    }

    // ---------------------------------------------------------------- 등록

    @Test
    @DisplayName("createPopup은 부모 공지와 분반을 기록하고 노출 상태로 저장한다")
    void createPopup_SavesEnabledWithParent() {
        // when
        Long id = createPopup();

        // then
        AnnouncementPopup saved = fakePopupRepository.findById(id).orElseThrow();
        assertThat(saved.getAnnouncementId()).isEqualTo(10L);
        assertThat(saved.getSectionId()).isEqualTo(1L);
        assertThat(saved.getContent()).isEqualTo("안내 내용");
        assertThat(saved.isEnabled()).isTrue();
        assertThat(saved.hasImage()).isFalse();
    }

    @Test
    @DisplayName("createPopup에 이미지가 함께 오면 파일을 저장하고 팝업에 연결한다")
    void createPopup_WithImage_LinksFileObject() {
        // when
        Long id = createPopupWithImage();

        // then
        AnnouncementPopup saved = fakePopupRepository.findById(id).orElseThrow();
        assertThat(saved.hasImage()).isTrue();
        assertThat(fakeFileObjectRepository.findById(saved.getImageFileId())).isPresent();
    }

    @Test
    @DisplayName("createPopup은 비어 있는 이미지 파트를 이미지 없음으로 처리한다")
    void createPopup_EmptyImagePart_TreatedAsNoImage() {
        // given
        MultipartFile empty = new MockMultipartFile("image", "빈파일.png", "image/png", new byte[0]);

        // when
        Long id = commandService.createPopup(10L, 1L, null, "안내 내용", empty, UPLOADER);

        // then
        assertThat(fakePopupRepository.findById(id).orElseThrow().hasImage()).isFalse();
    }

    @Test
    @DisplayName("createPopup은 내용이 공백이면 저장하지 않고 거부한다")
    void createPopup_BlankContent_Rejected() {
        assertThatThrownBy(() -> commandService.createPopup(10L, 1L, null, "   ", null, UPLOADER))
            .isInstanceOf(AnnouncementPopupInvalidContentException.class);
        assertThat(fakePopupRepository.existsByAnnouncementId(10L)).isFalse();
    }

    @Test
    @DisplayName("createPopup은 같은 공지에 팝업이 이미 있으면 거부한다")
    void createPopup_DuplicateAnnouncement_Rejected() {
        // given
        createPopup();

        // when & then
        assertThatThrownBy(() -> commandService.createPopup(10L, 1L, null, "다른 내용", null, UPLOADER))
            .isInstanceOf(AnnouncementPopupAlreadyExistsException.class);
    }

    @Test
    @DisplayName("createPopup은 이미지가 아닌 파일이면 팝업과 파일 객체를 만들지 않고 거부한다")
    void createPopup_NonImageFile_RejectedWithoutSideEffects() {
        // when & then
        assertThatThrownBy(() -> commandService.createPopup(
            10L, 1L, null, "안내 내용", image("안내문.pdf", "application/pdf"), UPLOADER))
            .isInstanceOf(FileObjectInvalidTypeException.class);
        assertThat(fakePopupRepository.existsByAnnouncementId(10L)).isFalse();
        assertThat(fakeFileObjectRepository.findById(1L)).isEmpty();
    }

    @Test
    @DisplayName("createPopup은 서로 다른 공지에는 각각 팝업을 만들 수 있다")
    void createPopup_DifferentAnnouncements_BothCreated() {
        // when
        Long first = commandService.createPopup(10L, 1L, null, "첫 번째", null, UPLOADER);
        Long second = commandService.createPopup(11L, 1L, null, "두 번째", null, UPLOADER);

        // then
        assertThat(first).isNotEqualTo(second);
    }

    // ---------------------------------------------------------------- 수정

    @Test
    @DisplayName("updatePopup은 요청에 포함된 내용만 바꾸고 나머지는 유지한다")
    void updatePopup_ContentOnly_KeepsOthers() {
        // given
        Long id = createPopupWithImage();
        Long imageFileId = fakePopupRepository.findById(id).orElseThrow().getImageFileId();

        // when
        commandService.updatePopup(id, null, "수정된 내용", null, false, null, UPLOADER);

        // then
        AnnouncementPopup updated = fakePopupRepository.findById(id).orElseThrow();
        assertThat(updated.getContent()).isEqualTo("수정된 내용");
        assertThat(updated.isEnabled()).isTrue();
        assertThat(updated.getImageFileId()).isEqualTo(imageFileId);
    }

    @Test
    @DisplayName("updatePopup은 enabled를 false로 보내면 노출을 끈다")
    void updatePopup_Disable() {
        // given
        Long id = createPopup();

        // when
        commandService.updatePopup(id, null, null, false, false, null, UPLOADER);

        // then
        assertThat(fakePopupRepository.findById(id).orElseThrow().isEnabled()).isFalse();
    }

    @Test
    @DisplayName("updatePopup은 꺼진 팝업을 다시 켤 수 있다")
    void updatePopup_ReEnable() {
        // given
        Long id = createPopup();
        commandService.updatePopup(id, null, null, false, false, null, UPLOADER);

        // when
        commandService.updatePopup(id, null, null, true, false, null, UPLOADER);

        // then
        assertThat(fakePopupRepository.findById(id).orElseThrow().isEnabled()).isTrue();
    }

    @Test
    @DisplayName("updatePopup은 새 이미지를 보내면 기존 이미지를 교체한다")
    void updatePopup_NewImage_ReplacesImage() {
        // given
        Long id = createPopupWithImage();
        Long oldFileId = fakePopupRepository.findById(id).orElseThrow().getImageFileId();

        // when
        commandService.updatePopup(id, null, null, null, false, image("새이미지.jpg", "image/jpeg"), UPLOADER);

        // then
        Long newFileId = fakePopupRepository.findById(id).orElseThrow().getImageFileId();
        assertThat(newFileId).isNotNull().isNotEqualTo(oldFileId);
    }

    @Test
    @DisplayName("updatePopup은 removeImage가 true면 이미지를 제거한다")
    void updatePopup_RemoveImage() {
        // given
        Long id = createPopupWithImage();

        // when
        commandService.updatePopup(id, null, null, null, true, null, UPLOADER);

        // then
        assertThat(fakePopupRepository.findById(id).orElseThrow().hasImage()).isFalse();
    }

    @Test
    @DisplayName("updatePopup은 이미지 교체와 제거를 함께 요청하면 거부하고 아무것도 바꾸지 않는다")
    void updatePopup_ReplaceAndRemove_Rejected() {
        // given
        Long id = createPopupWithImage();
        Long imageFileId = fakePopupRepository.findById(id).orElseThrow().getImageFileId();

        // when & then
        assertThatThrownBy(() -> commandService.updatePopup(id, null, "바뀌면 안 되는 내용", null, true, image("새이미지.png", "image/png"), UPLOADER))
            .isInstanceOf(AnnouncementPopupImageConflictException.class);
        AnnouncementPopup unchanged = fakePopupRepository.findById(id).orElseThrow();
        assertThat(unchanged.getContent()).isEqualTo("안내 내용");
        assertThat(unchanged.getImageFileId()).isEqualTo(imageFileId);
    }

    @Test
    @DisplayName("updatePopup은 바꿀 항목이 하나도 없으면 거부한다")
    void updatePopup_Empty_Rejected() {
        // given
        Long id = createPopup();

        // when & then
        assertThatThrownBy(() -> commandService.updatePopup(id, null, null, null, false, null, UPLOADER))
            .isInstanceOf(AnnouncementPopupEmptyUpdateException.class);
    }

    @Test
    @DisplayName("updatePopup은 내용을 공백으로 바꾸려 하면 거부하고 기존 내용을 유지한다")
    void updatePopup_BlankContent_Rejected() {
        // given
        Long id = createPopup();

        // when & then
        assertThatThrownBy(() -> commandService.updatePopup(id, null, "  ", null, false, null, UPLOADER))
            .isInstanceOf(AnnouncementPopupInvalidContentException.class);
        assertThat(fakePopupRepository.findById(id).orElseThrow().getContent()).isEqualTo("안내 내용");
    }

    @Test
    @DisplayName("updatePopup은 이미지가 아닌 파일을 보내면 거부하고 다른 수정도 반영하지 않는다")
    void updatePopup_NonImageFile_RejectedAtomically() {
        // given
        Long id = createPopup();

        // when & then
        assertThatThrownBy(() -> commandService.updatePopup(id, null, "바뀌면 안 되는 내용", false, false, image("안내문.pdf", "application/pdf"), UPLOADER))
            .isInstanceOf(FileObjectInvalidTypeException.class);
        AnnouncementPopup unchanged = fakePopupRepository.findById(id).orElseThrow();
        assertThat(unchanged.getContent()).isEqualTo("안내 내용");
        assertThat(unchanged.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("updatePopup은 없는 팝업이면 NotFound를 던진다")
    void updatePopup_NotFound() {
        assertThatThrownBy(() -> commandService.updatePopup(999L, null, "내용", null, false, null, UPLOADER))
            .isInstanceOf(AnnouncementPopupNotFoundException.class);
    }

    // ---------------------------------------------------------------- 제목(선택)

    @Test
    @DisplayName("createPopup은 제목을 보내면 저장하고, 보내지 않으면 제목 없음으로 저장한다")
    void createPopup_Title_Optional() {
        // when
        Long withTitle = commandService.createPopup(10L, 1L, "중간보고서 안내", "내용", null, UPLOADER);
        Long withoutTitle = commandService.createPopup(11L, 1L, null, "내용", null, UPLOADER);

        // then
        assertThat(fakePopupRepository.findById(withTitle).orElseThrow().getTitle()).isEqualTo("중간보고서 안내");
        assertThat(fakePopupRepository.findById(withoutTitle).orElseThrow().hasTitle()).isFalse();
    }

    @Test
    @DisplayName("createPopup은 공백뿐인 제목을 제목 없음(null)으로 저장한다")
    void createPopup_BlankTitle_StoredAsNull() {
        // when
        Long id = commandService.createPopup(10L, 1L, "   ", "내용", null, UPLOADER);

        // then
        assertThat(fakePopupRepository.findById(id).orElseThrow().getTitle()).isNull();
    }

    @Test
    @DisplayName("createPopup은 제목이 200자면 허용하고 201자면 저장하지 않고 거부한다")
    void createPopup_TitleLengthBoundary() {
        // when
        Long id = commandService.createPopup(10L, 1L, "가".repeat(200), "내용", null, UPLOADER);

        // then
        assertThat(fakePopupRepository.findById(id).orElseThrow().getTitle()).hasSize(200);
        assertThatThrownBy(() -> commandService.createPopup(11L, 1L, "가".repeat(201), "내용", null, UPLOADER))
            .isInstanceOf(AnnouncementPopupTitleTooLongException.class);
        assertThat(fakePopupRepository.existsByAnnouncementId(11L)).isFalse();
    }

    @Test
    @DisplayName("updatePopup은 제목만 보내도 수정으로 인정하고 다른 항목은 유지한다")
    void updatePopup_TitleOnly() {
        // given
        Long id = createPopup();

        // when
        commandService.updatePopup(id, "새 제목", null, null, false, null, UPLOADER);

        // then
        AnnouncementPopup updated = fakePopupRepository.findById(id).orElseThrow();
        assertThat(updated.getTitle()).isEqualTo("새 제목");
        assertThat(updated.getContent()).isEqualTo("안내 내용");
    }

    @Test
    @DisplayName("updatePopup은 제목을 보내지 않으면 기존 제목을 유지한다")
    void updatePopup_TitleOmitted_KeepsTitle() {
        // given
        Long id = commandService.createPopup(10L, 1L, "기존 제목", "안내 내용", null, UPLOADER);

        // when
        commandService.updatePopup(id, null, "수정된 내용", null, false, null, UPLOADER);

        // then
        assertThat(fakePopupRepository.findById(id).orElseThrow().getTitle()).isEqualTo("기존 제목");
    }

    @Test
    @DisplayName("updatePopup은 빈 문자열 제목을 보내면 제목을 지운다")
    void updatePopup_BlankTitle_ClearsTitle() {
        // given
        Long id = commandService.createPopup(10L, 1L, "기존 제목", "안내 내용", null, UPLOADER);

        // when
        commandService.updatePopup(id, "", null, null, false, null, UPLOADER);

        // then
        assertThat(fakePopupRepository.findById(id).orElseThrow().hasTitle()).isFalse();
    }

    @Test
    @DisplayName("updatePopup은 제목이 201자면 거부하고 다른 수정도 반영하지 않는다")
    void updatePopup_TitleTooLong_RejectedAtomically() {
        // given
        Long id = createPopup();

        // when & then
        assertThatThrownBy(() -> commandService.updatePopup(
            id, "가".repeat(201), "바뀌면 안 되는 내용", null, false, null, UPLOADER))
            .isInstanceOf(AnnouncementPopupTitleTooLongException.class);
        assertThat(fakePopupRepository.findById(id).orElseThrow().getContent()).isEqualTo("안내 내용");
    }
}
