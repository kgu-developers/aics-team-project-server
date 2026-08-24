package sectionannouncement.application.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.List;
import kgu.developers.common.exception.CustomException;
import kgu.developers.domain.sectionannouncement.application.query.SectionAnnouncementQueryService;
import kgu.developers.domain.sectionannouncement.domain.SectionAnnouncement;
import mock.repository.FakeSectionAnnouncementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SectionAnnouncementQueryServiceTest {

    private FakeSectionAnnouncementRepository fakeSectionAnnouncementRepository;
    private SectionAnnouncementQueryService queryService;

    @BeforeEach
    void init() {
        fakeSectionAnnouncementRepository = new FakeSectionAnnouncementRepository();
        queryService = new SectionAnnouncementQueryService(fakeSectionAnnouncementRepository);
    }

    private SectionAnnouncement save(Long sectionId) {
        return fakeSectionAnnouncementRepository.save(
            SectionAnnouncement.create(sectionId, "제목", "내용", LocalDateTime.now())
        );
    }

    @Test
    @DisplayName("getAnnouncement는 id로 공지사항을 조회한다")
    void getAnnouncement_Success() {
        // given
        SectionAnnouncement saved = save(1L);

        // when
        SectionAnnouncement found = queryService.getAnnouncement(saved.getId());

        // then
        assertThat(found.getId()).isEqualTo(saved.getId());
    }

    @Test
    @DisplayName("getAnnouncement는 존재하지 않는 id면 예외를 던진다")
    void getAnnouncement_NotFound_ThrowsException() {
        // when & then
        assertThatThrownBy(() -> queryService.getAnnouncement(999L))
            .isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("getAnnouncements는 분반의 공지사항 전체를 반환한다")
    void getAnnouncements_ReturnsAll() {
        // given
        save(1L);
        save(1L);
        save(2L);

        // when
        List<SectionAnnouncement> results = queryService.getAnnouncements(1L);

        // then
        assertThat(results).hasSize(2);
    }
}
