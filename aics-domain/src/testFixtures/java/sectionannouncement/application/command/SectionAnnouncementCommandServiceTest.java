package sectionannouncement.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.List;
import kgu.developers.common.exception.CustomException;
import kgu.developers.domain.sectionannouncement.application.command.SectionAnnouncementCommandService;
import kgu.developers.domain.sectionannouncement.domain.SectionAnnouncement;
import kgu.developers.domain.sectionannouncement.domain.SectionAnnouncementAttachment;
import mock.repository.FakeFileObjectRepository;
import mock.repository.FakeFileStorage;
import mock.repository.FakeSectionAnnouncementAttachmentRepository;
import mock.repository.FakeSectionAnnouncementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

class SectionAnnouncementCommandServiceTest {

    private FakeSectionAnnouncementRepository fakeSectionAnnouncementRepository;
    private FakeSectionAnnouncementAttachmentRepository fakeSectionAnnouncementAttachmentRepository;
    private SectionAnnouncementCommandService commandService;

    @BeforeEach
    void init() {
        fakeSectionAnnouncementRepository = new FakeSectionAnnouncementRepository();
        fakeSectionAnnouncementAttachmentRepository = new FakeSectionAnnouncementAttachmentRepository();
        commandService = new SectionAnnouncementCommandService(
            fakeSectionAnnouncementRepository,
            fakeSectionAnnouncementAttachmentRepository,
            new FakeFileObjectRepository(),
            new FakeFileStorage()
        );
    }

    private Long createAnnouncement() {
        return commandService.createAnnouncement(1L, "제목", "내용", LocalDateTime.now(), null, "202400001");
    }

    @Test
    @DisplayName("createAnnouncement는 저장된 공지사항의 id를 반환한다")
    void createAnnouncement_ReturnsSavedId() {
        // when
        Long id = createAnnouncement();

        // then
        assertThat(id).isNotNull();
        SectionAnnouncement saved = fakeSectionAnnouncementRepository.findById(id).orElseThrow();
        assertThat(saved.getTitle()).isEqualTo("제목");
    }

    @Test
    @DisplayName("createAnnouncement에 파일이 함께 오면 첨부파일로 연결된다")
    void createAnnouncement_WithFiles_CreatesAttachments() {
        // given
        MultipartFile file = new MockMultipartFile("file", "안내문.pdf", "application/pdf", "content".getBytes());

        // when
        Long id = commandService.createAnnouncement(1L, "제목", "내용", LocalDateTime.now(), List.of(file), "202400001");

        // then
        List<SectionAnnouncementAttachment> attachments = fakeSectionAnnouncementAttachmentRepository.findAllByAnnouncementId(id);
        assertThat(attachments).hasSize(1);
    }

    @Test
    @DisplayName("updateAnnouncement는 전달된 필드만 갱신한다")
    void updateAnnouncement_UpdatesOnlyProvidedFields() {
        // given
        Long id = createAnnouncement();

        // when
        commandService.updateAnnouncement(id, "수정된 제목", null, null, null, null, "202400001");

        // then
        SectionAnnouncement updated = fakeSectionAnnouncementRepository.findById(id).orElseThrow();
        assertThat(updated.getTitle()).isEqualTo("수정된 제목");
        assertThat(updated.getContent()).isEqualTo("내용");
    }

    @Test
    @DisplayName("updateAnnouncement는 텍스트 필드 없이 파일만 추가해도 동작한다")
    void updateAnnouncement_FilesOnly_AddsAttachment() {
        // given
        Long id = createAnnouncement();
        MultipartFile file = new MockMultipartFile("file", "추가자료.pdf", "application/pdf", "content".getBytes());

        // when
        commandService.updateAnnouncement(id, null, null, null, List.of(file), null, "202400001");

        // then
        assertThat(fakeSectionAnnouncementAttachmentRepository.findAllByAnnouncementId(id)).hasSize(1);
    }

    @Test
    @DisplayName("updateAnnouncement는 지정한 첨부파일을 삭제한다")
    void updateAnnouncement_DeleteAttachmentIds_RemovesAttachment() {
        // given
        Long id = createAnnouncement();
        MultipartFile file = new MockMultipartFile("file", "삭제될자료.pdf", "application/pdf", "content".getBytes());
        commandService.updateAnnouncement(id, null, null, null, List.of(file), null, "202400001");
        Long attachmentId = fakeSectionAnnouncementAttachmentRepository.findAllByAnnouncementId(id).get(0).getId();

        // when
        commandService.updateAnnouncement(id, null, null, null, null, List.of(attachmentId), "202400001");

        // then
        assertThat(fakeSectionAnnouncementAttachmentRepository.findAllByAnnouncementId(id)).isEmpty();
    }

    @Test
    @DisplayName("updateAnnouncement는 존재하지 않는 첨부파일을 삭제하려 하면 예외를 던진다")
    void updateAnnouncement_DeleteUnknownAttachmentId_ThrowsException() {
        // given
        Long id = createAnnouncement();

        // when & then
        assertThatThrownBy(() -> commandService.updateAnnouncement(id, null, null, null, null, List.of(999L), "202400001"))
            .isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("updateAnnouncement는 존재하지 않는 공지사항이면 예외를 던진다")
    void updateAnnouncement_NotFound_ThrowsException() {
        // when & then
        assertThatThrownBy(() -> commandService.updateAnnouncement(999L, "제목", null, null, null, null, "202400001"))
            .isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("updateAnnouncement는 공백만 있는 제목이면 예외를 던진다")
    void updateAnnouncement_BlankTitle_ThrowsException() {
        // given
        Long id = createAnnouncement();

        // when & then
        assertThatThrownBy(() -> commandService.updateAnnouncement(id, "   ", null, null, null, null, "202400001"))
            .isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("updateAnnouncement는 공백만 있는 내용이면 예외를 던진다")
    void updateAnnouncement_BlankContent_ThrowsException() {
        // given
        Long id = createAnnouncement();

        // when & then
        assertThatThrownBy(() -> commandService.updateAnnouncement(id, null, "   ", null, null, null, "202400001"))
            .isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("updateAnnouncement는 아무 필드도 전달되지 않으면 예외를 던진다")
    void updateAnnouncement_EmptyUpdate_ThrowsException() {
        // given
        Long id = createAnnouncement();

        // when & then
        assertThatThrownBy(() -> commandService.updateAnnouncement(id, null, null, null, null, null, "202400001"))
            .isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("markNotified는 알림 발송 시각을 기록한다")
    void markNotified_RecordsNotifiedAt() {
        // given
        Long id = createAnnouncement();
        LocalDateTime notifiedAt = LocalDateTime.now();

        // when
        commandService.markNotified(id, notifiedAt);

        // then
        SectionAnnouncement notified = fakeSectionAnnouncementRepository.findById(id).orElseThrow();
        assertThat(notified.isNotified()).isTrue();
        assertThat(notified.getNotifiedAt()).isEqualTo(notifiedAt);
    }

    @Test
    @DisplayName("markNotified는 존재하지 않는 공지사항이면 예외를 던진다")
    void markNotified_NotFound_ThrowsException() {
        // when & then
        assertThatThrownBy(() -> commandService.markNotified(999L, LocalDateTime.now()))
            .isInstanceOf(CustomException.class);
    }
}
