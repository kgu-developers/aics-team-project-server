package announcementpopup.presentation;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;
import kgu.developers.api.announcementpopup.application.StudentAnnouncementPopupFacade;
import kgu.developers.api.announcementpopup.presentation.StudentAnnouncementPopupControllerImpl;
import kgu.developers.api.announcementpopup.presentation.response.AnnouncementPopupDismissalResponse;
import kgu.developers.api.announcementpopup.presentation.response.StudentAnnouncementPopupListResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class StudentAnnouncementPopupControllerTest {

    private static final String USER_ID = "202412345";

    @Mock private StudentAnnouncementPopupFacade facade;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new StudentAnnouncementPopupControllerImpl(facade)).build();
    }

    @Test
    @DisplayName("GET 팝업 목록 API는 인증 사용자를 facade에 전달한다")
    void getPopups_UsesAuthenticatedUser() throws Exception {
        given(facade.getPopups(USER_ID)).willReturn(new StudentAnnouncementPopupListResponse(List.of()));

        mockMvc.perform(get("/api/v1/announcement-popups").principal(authentication()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.contents").isEmpty());

        then(facade).should().getPopups(USER_ID);
    }

    @Test
    @DisplayName("PUT 숨김 API는 인증 사용자와 팝업 버전을 facade에 전달한다")
    void dismiss_UsesAuthenticatedUserAndVersion() throws Exception {
        LocalDateTime hiddenUntil = LocalDateTime.of(2026, 12, 2, 10, 0);
        given(facade.dismiss(7L, USER_ID, 2L))
            .willReturn(new AnnouncementPopupDismissalResponse(7L, 2L, hiddenUntil));

        mockMvc.perform(put("/api/v1/announcement-popups/7/dismissal")
                .principal(authentication())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"version\":2}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.popupId").value(7L))
            .andExpect(jsonPath("$.version").value(2L));

        then(facade).should().dismiss(7L, USER_ID, 2L);
    }

    @Test
    @DisplayName("PUT 숨김 API는 version 필드가 없으면 400을 반환한다")
    void dismiss_MissingVersion_ReturnsBadRequest() throws Exception {
        assertInvalidVersion("{}");
    }

    @Test
    @DisplayName("PUT 숨김 API는 version이 null이면 400을 반환한다")
    void dismiss_NullVersion_ReturnsBadRequest() throws Exception {
        assertInvalidVersion("{\"version\":null}");
    }

    @Test
    @DisplayName("PUT 숨김 API는 version이 음수이면 400을 반환한다")
    void dismiss_NegativeVersion_ReturnsBadRequest() throws Exception {
        assertInvalidVersion("{\"version\":-1}");
    }

    private void assertInvalidVersion(String content) throws Exception {
        mockMvc.perform(put("/api/v1/announcement-popups/7/dismissal")
                .principal(authentication())
                .contentType(MediaType.APPLICATION_JSON)
                .content(content))
            .andExpect(status().isBadRequest());

        then(facade).shouldHaveNoInteractions();
    }

    private UsernamePasswordAuthenticationToken authentication() {
        return new UsernamePasswordAuthenticationToken(USER_ID, null, List.of());
    }
}
