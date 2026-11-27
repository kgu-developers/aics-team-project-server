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
import kgu.developers.domain.fileobject.domain.FileObject;
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
    private FakeFileStorage fakeFileStorage;
    private AnnouncementPopupCommandService commandService;

    @BeforeEach
    void init() {
        fakePopupRepository = new FakeAnnouncementPopupRepository();
        fakeFileObjectRepository = new FakeFileObjectRepository();
        fakeFileStorage = new FakeFileStorage();
        commandService = new AnnouncementPopupCommandService(
            fakePopupRepository, fakeFileObjectRepository, fakeFileStorage);
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

    // ---------------------------------------------------------------- Content-Type이 없는 이미지

    @Test
    @DisplayName("createPopup은 Content-Type이 없는 파일이면 500이 아니라 잘못된 파일로 거부하고 아무것도 만들지 않는다")
    void createPopup_NullContentType_RejectedWithoutSideEffects() {
        // given: 파일 파트에 Content-Type 헤더가 없는 경우
        MultipartFile noContentType = new MockMultipartFile("image", "안내.png", null, "bytes".getBytes());

        // when & then
        assertThatThrownBy(() -> commandService.createPopup(10L, 1L, null, "안내 내용", noContentType, UPLOADER))
            .isInstanceOf(FileObjectInvalidTypeException.class);
        assertThat(fakePopupRepository.existsByAnnouncementId(10L)).isFalse();
        assertThat(fakeFileStorage.storedKeys()).isEmpty();
    }

    @Test
    @DisplayName("updatePopup은 Content-Type이 없는 파일이면 거부하고 기존 팝업을 바꾸지 않는다")
    void updatePopup_NullContentType_RejectedAtomically() {
        // given
        Long id = createPopup();
        MultipartFile noContentType = new MockMultipartFile("image", "안내.png", null, "bytes".getBytes());

        // when & then
        assertThatThrownBy(() -> commandService.updatePopup(
            id, null, "바뀌면 안 되는 내용", null, false, noContentType, UPLOADER))
            .isInstanceOf(FileObjectInvalidTypeException.class);
        assertThat(fakePopupRepository.findById(id).orElseThrow().getContent()).isEqualTo("안내 내용");
        assertThat(fakeFileStorage.storedKeys()).isEmpty();
    }

    // ---------------------------------------------------------------- 저장 실패 시 업로드한 이미지 정리

    /** 팝업 저장만 실패하게 만든 저장소. 업로드 뒤 DB 저장이 실패하는 상황(유일 제약 위반, 동시 수정 충돌)을 흉내 낸다. */
    private static class FailingSaveRepository extends FakeAnnouncementPopupRepository {
        private boolean failOnSave;

        void failOnSave() {
            this.failOnSave = true;
        }

        @Override
        public AnnouncementPopup save(AnnouncementPopup announcementPopup) {
            if (failOnSave) {
                throw new AnnouncementPopupAlreadyExistsException();
            }
            return super.save(announcementPopup);
        }
    }

    @Test
    @DisplayName("createPopup은 이미지를 올린 뒤 팝업 저장이 실패하면 올린 이미지를 지우고 원래 예외를 던진다")
    void createPopup_SaveFails_DeletesUploadedImage() {
        // given
        FailingSaveRepository failingRepository = new FailingSaveRepository();
        failingRepository.failOnSave();
        AnnouncementPopupCommandService service = new AnnouncementPopupCommandService(
            failingRepository, fakeFileObjectRepository, fakeFileStorage);

        // when & then
        assertThatThrownBy(() -> service.createPopup(
            10L, 1L, null, "안내 내용", image("안내.png", "image/png"), UPLOADER))
            .isInstanceOf(AnnouncementPopupAlreadyExistsException.class);
        assertThat(fakeFileStorage.storedKeys()).isEmpty();
    }

    @Test
    @DisplayName("updatePopup은 새 이미지를 올린 뒤 저장이 실패하면 새로 올린 이미지만 지운다")
    void updatePopup_SaveFails_DeletesOnlyNewUpload() {
        // given: 기존 이미지가 있는 팝업(저장은 성공시켜 둔다)
        FailingSaveRepository failingRepository = new FailingSaveRepository();
        AnnouncementPopupCommandService service = new AnnouncementPopupCommandService(
            failingRepository, fakeFileObjectRepository, fakeFileStorage);
        Long id = service.createPopup(10L, 1L, null, "안내 내용", image("기존.png", "image/png"), UPLOADER);
        assertThat(fakeFileStorage.storedKeys()).hasSize(1);
        Long oldFileId = failingRepository.findById(id).orElseThrow().getImageFileId();

        // when: 교체 업로드 뒤 저장이 실패한다
        failingRepository.failOnSave();

        // then
        assertThatThrownBy(() -> service.updatePopup(
            id, null, null, null, false, image("새이미지.png", "image/png"), UPLOADER))
            .isInstanceOf(AnnouncementPopupAlreadyExistsException.class);
        assertThat(fakeFileStorage.storedKeys()).hasSize(1);
        assertThat(failingRepository.findById(id).orElseThrow().getImageFileId()).isEqualTo(oldFileId);
    }

    @Test
    @DisplayName("이미지 삭제까지 실패해도 원래 예외를 가리지 않고 삭제 실패는 suppressed로 남긴다")
    void createPopup_SaveFailsAndCleanupFails_KeepsOriginalException() {
        // given
        FailingSaveRepository failingRepository = new FailingSaveRepository();
        failingRepository.failOnSave();
        FakeFileStorage failingDeleteStorage = new FakeFileStorage() {
            @Override
            public void delete(String storageKey) {
                throw new IllegalStateException("삭제 실패");
            }
        };
        AnnouncementPopupCommandService service = new AnnouncementPopupCommandService(
            failingRepository, fakeFileObjectRepository, failingDeleteStorage);

        // when & then
        assertThatThrownBy(() -> service.createPopup(
            10L, 1L, null, "안내 내용", image("안내.png", "image/png"), UPLOADER))
            .isInstanceOf(AnnouncementPopupAlreadyExistsException.class)
            .satisfies(e -> assertThat(e.getSuppressed())
                .hasSize(1)
                .allSatisfy(s -> assertThat(s).isInstanceOf(IllegalStateException.class)));
    }

    @Test
    @DisplayName("이미지를 올린 뒤 파일 정보(FileObject) 저장이 실패해도 올린 이미지를 지운다")
    void createPopup_FileObjectSaveFails_DeletesUploadedImage() {
        // given
        FakeFileObjectRepository failingFileObjectRepository = new FakeFileObjectRepository() {
            @Override
            public FileObject save(FileObject fileObject) {
                throw new IllegalStateException("파일 정보 저장 실패");
            }
        };
        AnnouncementPopupCommandService service = new AnnouncementPopupCommandService(
            fakePopupRepository, failingFileObjectRepository, fakeFileStorage);

        // when & then
        assertThatThrownBy(() -> service.createPopup(
            10L, 1L, null, "안내 내용", image("안내.png", "image/png"), UPLOADER))
            .isInstanceOf(IllegalStateException.class);
        assertThat(fakeFileStorage.storedKeys()).isEmpty();
        assertThat(fakePopupRepository.existsByAnnouncementId(10L)).isFalse();
    }

    @Test
    @DisplayName("이미지 없이 등록하다 저장이 실패해도 지울 파일이 없어 그대로 예외만 던진다")
    void createPopup_SaveFailsWithoutImage_NoCleanupNeeded() {
        // given
        FailingSaveRepository failingRepository = new FailingSaveRepository();
        failingRepository.failOnSave();
        AnnouncementPopupCommandService service = new AnnouncementPopupCommandService(
            failingRepository, fakeFileObjectRepository, fakeFileStorage);

        // when & then
        assertThatThrownBy(() -> service.createPopup(10L, 1L, null, "안내 내용", null, UPLOADER))
            .isInstanceOf(AnnouncementPopupAlreadyExistsException.class);
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

    // ---------------------------------------------------------------- contentVersion(학생 숨김 기준)

    private long contentVersionOf(Long id) {
        return fakePopupRepository.findById(id).orElseThrow().getContentVersion();
    }

    @Test
    @DisplayName("contentVersion: 새로 등록한 팝업은 0이다")
    void contentVersion_Created_IsZero() {
        assertThat(contentVersionOf(createPopupWithImage())).isZero();
    }

    @Test
    @DisplayName("contentVersion: 노출을 껐다 켜기만 하면 오르지 않는다(학생의 숨김 유지)")
    void contentVersion_ToggleEnabled_Unchanged() {
        Long id = createPopup();
        commandService.updatePopup(id, null, null, false, false, null, UPLOADER);
        commandService.updatePopup(id, null, null, true, false, null, UPLOADER);
        assertThat(contentVersionOf(id)).isZero();
    }

    @Test
    @DisplayName("contentVersion: 같은 제목·내용을 다시 보내면 오르지 않는다")
    void contentVersion_SameValues_Unchanged() {
        Long id = commandService.createPopup(10L, 1L, "제목", "안내 내용", null, UPLOADER);
        commandService.updatePopup(id, "제목", "안내 내용", true, false, null, UPLOADER);
        assertThat(contentVersionOf(id)).isZero();
    }

    @Test
    @DisplayName("contentVersion: 제목이 없을 때 빈 제목을 보내면(둘 다 제목 없음) 오르지 않는다")
    void contentVersion_BlankTitleWhenNoTitle_Unchanged() {
        Long id = createPopup();
        commandService.updatePopup(id, "  ", null, null, false, null, UPLOADER);
        assertThat(contentVersionOf(id)).isZero();
    }

    @Test
    @DisplayName("contentVersion: 이미지가 없을 때 이미지 제거를 보내면 오르지 않는다")
    void contentVersion_RemoveImageWhenNone_Unchanged() {
        Long id = createPopup();
        commandService.updatePopup(id, null, null, null, true, null, UPLOADER);
        assertThat(contentVersionOf(id)).isZero();
    }

    @Test
    @DisplayName("contentVersion: 제목을 바꾸거나 지우면 1씩 오른다")
    void contentVersion_TitleChangedOrCleared_Increments() {
        Long id = commandService.createPopup(10L, 1L, "제목", "안내 내용", null, UPLOADER);
        commandService.updatePopup(id, "새 제목", null, null, false, null, UPLOADER);
        assertThat(contentVersionOf(id)).isEqualTo(1L);
        commandService.updatePopup(id, "", null, null, false, null, UPLOADER);
        assertThat(contentVersionOf(id)).isEqualTo(2L);
    }

    @Test
    @DisplayName("contentVersion: 내용을 바꾸면 노출 여부를 함께 바꿔도 한 번만 오른다")
    void contentVersion_ContentChangedWithToggle_IncrementsOnce() {
        Long id = createPopup();
        commandService.updatePopup(id, null, "수정된 내용", false, false, null, UPLOADER);
        assertThat(contentVersionOf(id)).isEqualTo(1L);
    }

    @Test
    @DisplayName("contentVersion: 제목과 내용을 한 번에 바꾸면 바뀐 항목마다 올라 2가 된다(학생 쪽은 값이 달라졌는지만 본다)")
    void contentVersion_TitleAndContentChanged_IncrementsPerField() {
        Long id = createPopup();
        commandService.updatePopup(id, "새 제목", "수정된 내용", null, false, null, UPLOADER);
        assertThat(contentVersionOf(id)).isEqualTo(2L);
    }

    @Test
    @DisplayName("contentVersion: 이미지를 교체하거나 제거하면 오른다")
    void contentVersion_ImageReplacedOrRemoved_Increments() {
        Long id = createPopupWithImage();
        commandService.updatePopup(id, null, null, null, false, image("새.png", "image/png"), UPLOADER);
        assertThat(contentVersionOf(id)).isEqualTo(1L);
        commandService.updatePopup(id, null, null, null, true, null, UPLOADER);
        assertThat(contentVersionOf(id)).isEqualTo(2L);
    }

    @Test
    @DisplayName("contentVersion: 저장이 거부되면(제목 길이 초과) 오르지 않는다")
    void contentVersion_RejectedUpdate_Unchanged() {
        Long id = createPopup();
        assertThatThrownBy(() -> commandService.updatePopup(
            id, "가".repeat(201), "바뀌면 안 되는 내용", null, false, null, UPLOADER))
            .isInstanceOf(AnnouncementPopupTitleTooLongException.class);
        assertThat(contentVersionOf(id)).isZero();
    }
}
