package evaluation.presentation;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import kgu.developers.admin.evaluation.application.PeerEvaluationFormFacade;
import kgu.developers.admin.config.SecurityConfig;
import kgu.developers.admin.evaluation.presentation.PeerEvaluationFormControllerImpl;
import kgu.developers.admin.evaluation.presentation.request.PeerEvaluationFormCreateRequest;
import kgu.developers.admin.evaluation.presentation.response.PeerEvaluationFormPersistResponse;
import kgu.developers.common.exception.GlobalExceptionHandler;
import kgu.developers.common.config.CorsConfig;
import kgu.developers.globalutils.jwt.JwtCookieAuthenticationFilter;
import kgu.developers.globalutils.jwt.JwtUtil;
import kgu.developers.globalutils.jwt.TokenRevocationStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest
@Import({
        SecurityConfig.class,
        JwtCookieAuthenticationFilter.class,
        JwtUtil.class,
        CorsConfig.class,
        PeerEvaluationFormControllerImpl.class,
        GlobalExceptionHandler.class
})
@TestPropertySource(properties = {
        "jwt.secret_key=local-dev-jwt-secret-key-0123456789",
        "jwt.issuer=kgudevelopers@gmail.com",
        "cors.allowed-origins=http://localhost:5173",
        "spring.security.user.name=admin",
        "spring.security.user.password=admin"
})
class PeerEvaluationFormControllerTest {

    private static final String URL =
            "/api/v1/admin/sections/{sectionId}/peer-evaluation-forms";
    private static final String VALID_BODY =
            """
            {
              "milestoneId":3,
              "anonymous":true,
              "opensAt":"2026-10-01T09:00:00",
              "closesAt":"2026-10-08T23:59:59"
            }
            """;

    @SpringBootConfiguration
    static class TestApp {
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PeerEvaluationFormFacade facade;

    @MockitoBean
    private TokenRevocationStore tokenRevocationStore;

    @Test
    @DisplayName("미인증 사용자는 상호평가 양식 API에 접근할 수 없다")
    void unauthenticated() throws Exception {
        mockMvc.perform(post(URL, 2L).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isUnauthorized());

        then(facade).shouldHaveNoInteractions();
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("일반 사용자는 상호평가 양식 API에 접근할 수 없다")
    void userForbidden() throws Exception {
        mockMvc.perform(post(URL, 2L).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isForbidden());

        then(facade).shouldHaveNoInteractions();
    }

    @Test
    @WithMockUser(username = "202012345", roles = "ADMIN")
    @DisplayName("유효한 상호평가 양식 생성 요청은 201을 응답한다")
    void createForm() throws Exception {
        given(facade.createForm(
                2L,
                "202012345",
                new PeerEvaluationFormCreateRequest(
                        3L,
                        true,
                        java.time.LocalDateTime.of(2026, 10, 1, 9, 0),
                        java.time.LocalDateTime.of(2026, 10, 8, 23, 59, 59))))
                .willReturn(PeerEvaluationFormPersistResponse.of(1L));

        mockMvc.perform(post(URL, 2L).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    @WithMockUser(username = "202012345", roles = "ADMIN")
    @DisplayName("담당 교수가 아닌 관리자는 상호평가 양식을 생성할 수 없다")
    void anotherProfessorForbidden() throws Exception {
        willThrow(new AccessDeniedException("담당 분반만 접근할 수 있습니다."))
                .given(facade)
                .createForm(
                        org.mockito.ArgumentMatchers.eq(2L),
                        org.mockito.ArgumentMatchers.eq("202012345"),
                        org.mockito.ArgumentMatchers.any());

        mockMvc.perform(post(URL, 2L).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @ParameterizedTest(name = "sectionId={0}")
    @ValueSource(longs = {0L, -1L})
    @WithMockUser(roles = "ADMIN")
    @DisplayName("0 이하 분반 id는 400을 응답한다")
    void rejectNonPositiveSectionId(long sectionId) throws Exception {
        mockMvc.perform(post(URL, sectionId).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));

        then(facade).shouldHaveNoInteractions();
    }

    @ParameterizedTest(name = "body={0}")
    @ValueSource(strings = {
            "{\"milestoneId\":0,\"anonymous\":true,\"opensAt\":\"2026-10-01T09:00:00\",\"closesAt\":\"2026-10-08T23:59:59\"}",
            "{\"milestoneId\":3,\"opensAt\":\"2026-10-01T09:00:00\",\"closesAt\":\"2026-10-08T23:59:59\"}",
            "{\"milestoneId\":3,\"anonymous\":true,\"opensAt\":\"2026-10-08T23:59:59\",\"closesAt\":\"2026-10-01T09:00:00\"}"
    })
    @WithMockUser(roles = "ADMIN")
    @DisplayName("유효하지 않은 상호평가 양식 생성 요청은 400을 응답한다")
    void rejectInvalidRequest(String body) throws Exception {
        mockMvc.perform(post(URL, 2L).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));

        then(facade).shouldHaveNoInteractions();
    }
}
