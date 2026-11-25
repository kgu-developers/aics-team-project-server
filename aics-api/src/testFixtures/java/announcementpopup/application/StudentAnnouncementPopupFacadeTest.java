package announcementpopup.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import kgu.developers.api.announcementpopup.application.StudentAnnouncementPopupFacade;
import kgu.developers.api.announcementpopup.presentation.response.AnnouncementPopupDismissalResponse;
import kgu.developers.api.announcementpopup.presentation.response.StudentAnnouncementPopupListResponse;
import kgu.developers.domain.announcementpopup.domain.AnnouncementPopup;
import kgu.developers.domain.announcementpopup.domain.AnnouncementPopupDismissal;
import kgu.developers.domain.announcementpopup.domain.AnnouncementPopupDismissalRepository;
import kgu.developers.domain.announcementpopup.domain.StudentAnnouncementPopupRepository;
import kgu.developers.domain.announcementpopup.exception.AnnouncementPopupConcurrentlyModifiedException;
import kgu.developers.domain.announcementpopup.exception.AnnouncementPopupNotFoundException;
import kgu.developers.domain.fileobject.domain.FileObject;
import kgu.developers.domain.fileobject.domain.FileObjectRepository;
import kgu.developers.domain.fileobject.domain.FileStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StudentAnnouncementPopupFacadeTest {

    private static final String USER_ID = "202412345";
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final Instant FIXED_INSTANT = Instant.parse("2026-11-25T01:00:00Z");
    private static final LocalDateTime NOW = LocalDateTime.ofInstant(FIXED_INSTANT, KST);

    @Mock private StudentAnnouncementPopupRepository popupRepository;
    @Mock private AnnouncementPopupDismissalRepository dismissalRepository;
    @Mock private FileObjectRepository fileObjectRepository;
    @Mock private FileStorage fileStorage;

    private StudentAnnouncementPopupFacade facade;

    @BeforeEach
    void setUp() {
        facade = new StudentAnnouncementPopupFacade(
            popupRepository,
            dismissalRepository,
            fileObjectRepository,
            fileStorage,
            Clock.fixed(FIXED_INSTANT, KST)
        );
    }

    @Test
    @DisplayName("조회 가능한 팝업이 없으면 빈 목록을 반환하고 파일을 조회하지 않는다")
    void getPopups_NoAvailablePopup_ReturnsEmpty() {
        given(dismissalRepository.findAllByUserId(USER_ID)).willReturn(List.of());
        given(popupRepository.findAvailable(USER_ID, null, NOW)).willReturn(List.of());

        StudentAnnouncementPopupListResponse response = facade.getPopups(USER_ID);

        assertThat(response.contents()).isEmpty();
        then(fileObjectRepository).shouldHaveNoInteractions();
        then(fileStorage).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("현재 버전 팝업을 숨김 기간 안에 조회하면 노출하지 않는다")
    void getPopups_CurrentVersionDismissed_HidesPopup() {
        AnnouncementPopup popup = popup(1L, 3L, null);
        given(dismissalRepository.findAllByUserId(USER_ID)).willReturn(List.of(
            new AnnouncementPopupDismissal(USER_ID, 1L, 3L, NOW.plusSeconds(1))
        ));
        given(popupRepository.findAvailable(USER_ID, null, NOW)).willReturn(List.of(popup));

        StudentAnnouncementPopupListResponse response = facade.getPopups(USER_ID);

        assertThat(response.contents()).isEmpty();
    }

    @Test
    @DisplayName("숨김 만료 시각과 현재 시각이 같으면 팝업을 다시 노출한다")
    void getPopups_DismissalExpiresAtNow_ResurfacesPopup() {
        AnnouncementPopup popup = popup(1L, 3L, null);
        given(dismissalRepository.findAllByUserId(USER_ID)).willReturn(List.of(
            new AnnouncementPopupDismissal(USER_ID, 1L, 3L, NOW)
        ));
        given(popupRepository.findAvailable(USER_ID, null, NOW)).willReturn(List.of(popup));

        StudentAnnouncementPopupListResponse response = facade.getPopups(USER_ID);

        assertThat(response.contents()).extracting("id").containsExactly(1L);
    }

    @Test
    @DisplayName("숨긴 뒤 팝업 버전이 바뀌면 숨김 기간 안이어도 다시 노출한다")
    void getPopups_ChangedVersion_ResurfacesPopup() {
        AnnouncementPopup popup = popup(1L, 4L, null);
        given(dismissalRepository.findAllByUserId(USER_ID)).willReturn(List.of(
            new AnnouncementPopupDismissal(USER_ID, 1L, 3L, NOW.plusDays(1))
        ));
        given(popupRepository.findAvailable(USER_ID, null, NOW)).willReturn(List.of(popup));

        StudentAnnouncementPopupListResponse response = facade.getPopups(USER_ID);

        assertThat(response.contents()).singleElement().satisfies(content -> {
            assertThat(content.id()).isEqualTo(1L);
            assertThat(content.version()).isEqualTo(4L);
        });
    }

    @Test
    @DisplayName("이미지는 중복 ID를 제거해 일괄 조회하고 이미지 없는 팝업에는 URL을 만들지 않는다")
    void getPopups_BatchesImages_AndLeavesNoImageNull() {
        AnnouncementPopup first = popup(1L, 0L, 101L);
        AnnouncementPopup noImage = popup(2L, 0L, null);
        AnnouncementPopup sameImage = popup(3L, 0L, 101L);
        FileObject file = FileObject.builder().id(101L).storageKey("popups/guide.png").build();
        given(dismissalRepository.findAllByUserId(USER_ID)).willReturn(List.of());
        given(popupRepository.findAvailable(USER_ID, null, NOW)).willReturn(List.of(first, noImage, sameImage));
        given(fileObjectRepository.findAllById(List.of(101L))).willReturn(List.of(file));
        given(fileStorage.presignedUrl("popups/guide.png")).willReturn("https://files.example/guide.png");

        StudentAnnouncementPopupListResponse response = facade.getPopups(USER_ID);

        assertThat(response.contents()).extracting("imageUrl")
            .containsExactly("https://files.example/guide.png", null, "https://files.example/guide.png");
        then(fileObjectRepository).should().findAllById(List.of(101L));
        then(fileStorage).should().presignedUrl("popups/guide.png");
    }

    @Test
    @DisplayName("숨김 요청은 인증 사용자와 현재 팝업 버전을 저장한다")
    void dismiss_CurrentVersion_SavesAuthenticatedUser() {
        AnnouncementPopup popup = popup(7L, 2L, null);
        given(popupRepository.findAvailable(USER_ID, 7L, NOW)).willReturn(List.of(popup));
        given(dismissalRepository.save(any(AnnouncementPopupDismissal.class), eq(NOW)))
            .willAnswer(invocation -> invocation.getArgument(0));

        AnnouncementPopupDismissalResponse response = facade.dismiss(7L, USER_ID, 2L);

        assertThat(response.popupId()).isEqualTo(7L);
        assertThat(response.version()).isEqualTo(2L);
        assertThat(response.hiddenUntil()).isEqualTo(NOW.plusDays(7));
        ArgumentCaptor<AnnouncementPopupDismissal> captor = ArgumentCaptor.forClass(AnnouncementPopupDismissal.class);
        then(dismissalRepository).should().save(captor.capture(), eq(NOW));
        assertThat(captor.getValue()).isEqualTo(
            new AnnouncementPopupDismissal(USER_ID, 7L, 2L, NOW.plusDays(7))
        );
    }

    @Test
    @DisplayName("사용자가 접근할 수 없는 팝업 숨김 요청은 404 예외를 발생시킨다")
    void dismiss_InaccessiblePopup_ThrowsNotFound() {
        given(popupRepository.findAvailable(USER_ID, 7L, NOW)).willReturn(List.of());

        assertThatThrownBy(() -> facade.dismiss(7L, USER_ID, 2L))
            .isInstanceOf(AnnouncementPopupNotFoundException.class);

        then(dismissalRepository).should(never()).save(any(), any());
    }

    @Test
    @DisplayName("오래된 버전의 숨김 요청은 충돌 예외를 발생시키고 저장하지 않는다")
    void dismiss_StaleVersion_ThrowsConflictWithoutSave() {
        given(popupRepository.findAvailable(USER_ID, 7L, NOW)).willReturn(List.of(popup(7L, 3L, null)));

        assertThatThrownBy(() -> facade.dismiss(7L, USER_ID, 2L))
            .isInstanceOf(AnnouncementPopupConcurrentlyModifiedException.class);

        then(dismissalRepository).should(never()).save(any(), any());
    }

    private AnnouncementPopup popup(Long id, long version, Long imageFileId) {
        return AnnouncementPopup.builder()
            .id(id)
            .announcementId(100L + id)
            .sectionId(10L)
            .title("안내 " + id)
            .content("팝업 내용")
            .imageFileId(imageFileId)
            .enabled(true)
            .version(version)
            .createdAt(NOW.minusDays(1))
            .updatedAt(NOW.minusHours(1))
            .build();
    }
}
