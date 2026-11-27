package announcementpopup.application.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import kgu.developers.domain.announcementpopup.application.query.AnnouncementPopupQueryService;
import kgu.developers.domain.announcementpopup.domain.AnnouncementPopup;
import kgu.developers.domain.announcementpopup.exception.AnnouncementPopupNotFoundException;
import mock.repository.FakeAnnouncementPopupRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AnnouncementPopupQueryServiceTest {

    private FakeAnnouncementPopupRepository fakePopupRepository;
    private AnnouncementPopupQueryService queryService;

    @BeforeEach
    void init() {
        fakePopupRepository = new FakeAnnouncementPopupRepository();
        queryService = new AnnouncementPopupQueryService(fakePopupRepository);
    }

    private AnnouncementPopup savePopup(Long announcementId, Long sectionId, String content) {
        return fakePopupRepository.save(AnnouncementPopup.create(announcementId, sectionId, null, content, null));
    }

    @Test
    @DisplayName("getPopup은 저장된 팝업을 반환한다")
    void getPopup_ReturnsSavedPopup() {
        // given
        AnnouncementPopup saved = savePopup(10L, 1L, "안내 내용");

        // when
        AnnouncementPopup found = queryService.getPopup(saved.getId());

        // then
        assertThat(found.getAnnouncementId()).isEqualTo(10L);
        assertThat(found.getContent()).isEqualTo("안내 내용");
    }

    @Test
    @DisplayName("getPopup은 없는 팝업이면 NotFound를 던진다")
    void getPopup_NotFound() {
        assertThatThrownBy(() -> queryService.getPopup(999L))
            .isInstanceOf(AnnouncementPopupNotFoundException.class);
    }

    @Test
    @DisplayName("getPopups는 해당 분반의 팝업만 최신순으로 반환한다")
    void getPopups_OwnSectionNewestFirst() {
        // given
        AnnouncementPopup first = savePopup(10L, 1L, "첫 번째");
        AnnouncementPopup second = savePopup(11L, 1L, "두 번째");
        savePopup(12L, 2L, "다른 분반");

        // when
        List<AnnouncementPopup> popups = queryService.getPopups(1L);

        // then
        assertThat(popups).extracting(AnnouncementPopup::getId).containsExactly(second.getId(), first.getId());
    }

    @Test
    @DisplayName("getPopups는 꺼진 팝업도 포함해서 반환한다(교수 관리 화면용)")
    void getPopups_IncludesDisabled() {
        // given
        AnnouncementPopup popup = savePopup(10L, 1L, "내용");
        popup.changeEnabled(false);
        fakePopupRepository.save(popup);

        // when
        List<AnnouncementPopup> popups = queryService.getPopups(1L);

        // then
        assertThat(popups).hasSize(1);
        assertThat(popups.get(0).isEnabled()).isFalse();
    }

    @Test
    @DisplayName("getPopups는 팝업이 없는 분반이면 빈 목록을 반환한다")
    void getPopups_Empty() {
        assertThat(queryService.getPopups(1L)).isEmpty();
    }
}
