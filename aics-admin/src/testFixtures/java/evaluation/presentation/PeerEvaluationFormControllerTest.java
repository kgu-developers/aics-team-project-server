package evaluation.presentation;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import kgu.developers.admin.evaluation.application.PeerEvaluationFormFacade;
import kgu.developers.admin.config.SecurityConfig;
import kgu.developers.admin.evaluation.presentation.PeerEvaluationFormControllerImpl;
import kgu.developers.admin.evaluation.presentation.request.PeerEvaluationFormCreateRequest;
import kgu.developers.admin.evaluation.presentation.request.PeerEvaluationFormUpdateRequest;
import kgu.developers.admin.evaluation.presentation.response.PeerEvaluationFormPersistResponse;
import kgu.developers.admin.evaluation.presentation.response.PeerEvaluationFormResponse;
import kgu.developers.common.exception.GlobalExceptionHandler;
import kgu.developers.common.config.CorsConfig;
import kgu.developers.globalutils.jwt.JwtCookieAuthenticationFilter;
import kgu.developers.globalutils.jwt.JwtUtil;
import kgu.developers.globalutils.jwt.TokenRevocationStore;
import kgu.developers.globalutils.jwt.PasswordChangeRequirementChecker;
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
    private static final String UPDATE_URL =
            "/api/v1/admin/sections/{sectionId}/peer-evaluation-forms/{formId}";
    private static final String MILESTONE_FORM_URL =
            "/api/v1/admin/sections/{sectionId}/peer-evaluation-forms/milestones/{milestoneId}";
    private static final String VALID_UPDATE_BODY =
            """
            {
              "anonymous":false,
              "opensAt":"2026-10-02T09:00:00",
              "closesAt":"2026-10-09T23:59:59"
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

    @MockitoBean
    private PasswordChangeRequirementChecker passwordChangeRequirementChecker;

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

    @Test
    @WithMockUser(username = "202012345", roles = "ADMIN")
    @DisplayName("유효한 상호평가 양식 수정 요청은 204를 응답한다")
    void updateForm() throws Exception {
        mockMvc.perform(put(UPDATE_URL, 2L, 1L).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_UPDATE_BODY))
                .andExpect(status().isNoContent());

        then(facade).should().updateForm(
                2L,
                "202012345",
                1L,
                new PeerEvaluationFormUpdateRequest(
                        false,
                        java.time.LocalDateTime.of(2026, 10, 2, 9, 0),
                        java.time.LocalDateTime.of(2026, 10, 9, 23, 59, 59)));
    }

    @Test
    @WithMockUser(username = "202012345", roles = "ADMIN")
    @DisplayName("담당 교수가 아닌 관리자는 상호평가 양식을 수정할 수 없다")
    void updateAnotherProfessorForbidden() throws Exception {
        willThrow(new AccessDeniedException("담당 분반만 접근할 수 있습니다."))
                .given(facade)
                .updateForm(
                        org.mockito.ArgumentMatchers.eq(2L),
                        org.mockito.ArgumentMatchers.eq("202012345"),
                        org.mockito.ArgumentMatchers.eq(1L),
                        org.mockito.ArgumentMatchers.any());

        mockMvc.perform(put(UPDATE_URL, 2L, 1L).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_UPDATE_BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @ParameterizedTest(name = "sectionId={0}, formId={1}")
    @org.junit.jupiter.params.provider.CsvSource({"0, 1", "-1, 1", "1, 0", "1, -1"})
    @WithMockUser(roles = "ADMIN")
    @DisplayName("0 이하의 분반 id 또는 양식 id는 400을 응답한다")
    void rejectNonPositiveSectionIdOrFormIdOnUpdate(long sectionId, long formId) throws Exception {
        mockMvc.perform(put(UPDATE_URL, sectionId, formId).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_UPDATE_BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));

        then(facade).shouldHaveNoInteractions();
    }

    @ParameterizedTest(name = "body={0}")
    @ValueSource(strings = {
            "{\"opensAt\":\"2026-10-01T09:00:00\",\"closesAt\":\"2026-10-08T23:59:59\"}",
            "{\"anonymous\":true,\"closesAt\":\"2026-10-08T23:59:59\"}",
            "{\"anonymous\":true,\"opensAt\":\"2026-10-01T09:00:00\"}",
            "{\"anonymous\":true,\"opensAt\":\"2026-10-08T23:59:59\",\"closesAt\":\"2026-10-01T09:00:00\"}"
    })
    @WithMockUser(roles = "ADMIN")
    @DisplayName("유효하지 않은 상호평가 양식 수정 요청은 400을 응답한다")
    void rejectInvalidUpdateRequest(String body) throws Exception {
        mockMvc.perform(put(UPDATE_URL, 2L, 1L).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));

        then(facade).shouldHaveNoInteractions();
    }

    @Test
    @WithMockUser(username = "202012345", roles = "ADMIN")
    @DisplayName("양식 ID로 상호평가 양식을 조회하면 200을 응답한다")
    void getForm() throws Exception {
        given(facade.getForm(2L, "202012345", 1L))
                .willReturn(new PeerEvaluationFormResponse(
                        1L,
                        2L,
                        3L,
                        true,
                        java.time.LocalDateTime.of(2026, 10, 1, 9, 0),
                        java.time.LocalDateTime.of(2026, 10, 8, 23, 59, 59)));

        mockMvc.perform(get(UPDATE_URL, 2L, 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.sectionId").value(2L))
                .andExpect(jsonPath("$.milestoneId").value(3L))
                .andExpect(jsonPath("$.anonymous").value(true));
    }

    @ParameterizedTest(name = "sectionId={0}, formId={1}")
    @org.junit.jupiter.params.provider.CsvSource({"0, 1", "-1, 1", "1, 0", "1, -1"})
    @WithMockUser(roles = "ADMIN")
    @DisplayName("0 이하의 분반 id 또는 양식 id로 조회 시 400을 응답한다")
    void rejectNonPositiveSectionIdOrFormIdOnGet(long sectionId, long formId) throws Exception {
        mockMvc.perform(get(UPDATE_URL, sectionId, formId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));

        then(facade).shouldHaveNoInteractions();
    }

    @Test
    @WithMockUser(username = "202012345", roles = "ADMIN")
    @DisplayName("마일스톤 ID로 상호평가 양식을 조회하면 200을 응답한다")
    void getFormByMilestoneId() throws Exception {
        given(facade.getFormByMilestoneId(2L, "202012345", 3L))
                .willReturn(new PeerEvaluationFormResponse(
                        1L,
                        2L,
                        3L,
                        true,
                        java.time.LocalDateTime.of(2026, 10, 1, 9, 0),
                        java.time.LocalDateTime.of(2026, 10, 8, 23, 59, 59)));

        mockMvc.perform(get(MILESTONE_FORM_URL, 2L, 3L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.sectionId").value(2L))
                .andExpect(jsonPath("$.milestoneId").value(3L))
                .andExpect(jsonPath("$.anonymous").value(true));
    }

    @ParameterizedTest(name = "sectionId={0}, milestoneId={1}")
    @org.junit.jupiter.params.provider.CsvSource({"0, 3", "-1, 3", "2, 0", "2, -1"})
    @WithMockUser(roles = "ADMIN")
    @DisplayName("0 이하의 분반 id 또는 마일스톤 id로 조회 시 400을 응답한다")
    void rejectNonPositiveSectionIdOrMilestoneIdOnGet(long sectionId, long milestoneId) throws Exception {
        mockMvc.perform(get(MILESTONE_FORM_URL, sectionId, milestoneId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));

        then(facade).shouldHaveNoInteractions();
    }

    @Test
    @WithMockUser(username = "202012345", roles = "ADMIN")
    @DisplayName("마일스톤 ID로 상호평가 양식 수정 요청은 204를 응답한다")
    void updateFormByMilestoneId() throws Exception {
        mockMvc.perform(put(MILESTONE_FORM_URL, 2L, 3L).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_UPDATE_BODY))
                .andExpect(status().isNoContent());

        then(facade).should().updateFormByMilestoneId(
                2L,
                "202012345",
                3L,
                new PeerEvaluationFormUpdateRequest(
                        false,
                        java.time.LocalDateTime.of(2026, 10, 2, 9, 0),
                        java.time.LocalDateTime.of(2026, 10, 9, 23, 59, 59)));
    }

    @ParameterizedTest(name = "sectionId={0}, milestoneId={1}")
    @org.junit.jupiter.params.provider.CsvSource({"0, 3", "-1, 3", "2, 0", "2, -1"})
    @WithMockUser(roles = "ADMIN")
    @DisplayName("0 이하의 분반 id 또는 마일스톤 id로 수정 시 400을 응답한다")
    void rejectNonPositiveSectionIdOrMilestoneIdOnUpdate(long sectionId, long milestoneId) throws Exception {
        mockMvc.perform(put(MILESTONE_FORM_URL, sectionId, milestoneId).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_UPDATE_BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));

        then(facade).shouldHaveNoInteractions();
    }

    @Test
    @WithMockUser(username = "202012345", roles = "ADMIN")
    @DisplayName("담당 교수가 아닌 관리자는 마일스톤 ID로 상호평가 양식을 수정할 수 없다")
    void updateFormByMilestoneIdAnotherProfessorForbidden() throws Exception {
        willThrow(new AccessDeniedException("담당 분반만 접근할 수 있습니다."))
                .given(facade)
                .updateFormByMilestoneId(
                        org.mockito.ArgumentMatchers.eq(2L),
                        org.mockito.ArgumentMatchers.eq("202012345"),
                        org.mockito.ArgumentMatchers.eq(3L),
                        org.mockito.ArgumentMatchers.any());

        mockMvc.perform(put(MILESTONE_FORM_URL, 2L, 3L).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_UPDATE_BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }
}
