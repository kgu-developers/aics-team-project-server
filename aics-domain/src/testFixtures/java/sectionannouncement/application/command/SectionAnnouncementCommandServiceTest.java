package sectionannouncement.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import kgu.developers.common.exception.CustomException;
import kgu.developers.domain.sectionannouncement.application.command.SectionAnnouncementCommandService;
import kgu.developers.domain.sectionannouncement.domain.SectionAnnouncement;
import mock.repository.FakeSectionAnnouncementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SectionAnnouncementCommandServiceTest {

    private FakeSectionAnnouncementRepository fakeSectionAnnouncementRepository;
    private SectionAnnouncementCommandService commandService;

    @BeforeEach
    void init() {
        fakeSectionAnnouncementRepository = new FakeSectionAnnouncementRepository();
        commandService = new SectionAnnouncementCommandService(fakeSectionAnnouncementRepository);
    }

    private Long createAnnouncement() {
        return commandService.createAnnouncement(1L, "제목", "내용", LocalDateTime.now());
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
    @DisplayName("updateAnnouncement는 전달된 필드만 갱신한다")
    void updateAnnouncement_UpdatesOnlyProvidedFields() {
        // given
        Long id = createAnnouncement();

        // when
        commandService.updateAnnouncement(id, "수정된 제목", null, null);

        // then
        SectionAnnouncement updated = fakeSectionAnnouncementRepository.findById(id).orElseThrow();
        assertThat(updated.getTitle()).isEqualTo("수정된 제목");
        assertThat(updated.getContent()).isEqualTo("내용");
    }

    @Test
    @DisplayName("updateAnnouncement는 존재하지 않는 공지사항이면 예외를 던진다")
    void updateAnnouncement_NotFound_ThrowsException() {
        // when & then
        assertThatThrownBy(() -> commandService.updateAnnouncement(999L, "제목", null, null))
            .isInstanceOf(CustomException.class);
    }
}
