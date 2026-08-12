package evaluation.presentation;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import kgu.developers.admin.evaluation.application.PeerEvaluationFormFacade;
import kgu.developers.admin.evaluation.presentation.PeerEvaluationFormControllerImpl;
import kgu.developers.admin.evaluation.presentation.request.PeerEvaluationFormCreateRequest;
import kgu.developers.admin.evaluation.presentation.response.PeerEvaluationFormPersistResponse;
import kgu.developers.common.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest
@Import({PeerEvaluationFormControllerImpl.class, GlobalExceptionHandler.class})
@WithMockUser(roles = "PROFESSOR")
@TestPropertySource(properties = {
        "spring.security.user.name=admin",
        "spring.security.user.password=admin"
})
class PeerEvaluationFormControllerTest {

    private static final String URL =
            "/api/v1/admin/oop/sections/{sectionId}/peer-evaluation-forms";
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

    @Test
    @DisplayName("유효한 상호평가 양식 생성 요청은 201을 응답한다")
    void createForm() throws Exception {
        given(facade.createForm(
                2L,
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

    @ParameterizedTest(name = "sectionId={0}")
    @ValueSource(longs = {0L, -1L})
    @DisplayName("0 이하 분반 id는 400을 응답한다")
    void rejectNonPositiveSectionId(long sectionId) throws Exception {
        mockMvc.perform(post(URL, sectionId).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

        then(facade).shouldHaveNoInteractions();
    }

    @ParameterizedTest(name = "body={0}")
    @ValueSource(strings = {
            "{\"milestoneId\":0,\"anonymous\":true,\"opensAt\":\"2026-10-01T09:00:00\",\"closesAt\":\"2026-10-08T23:59:59\"}",
            "{\"milestoneId\":3,\"opensAt\":\"2026-10-01T09:00:00\",\"closesAt\":\"2026-10-08T23:59:59\"}",
            "{\"milestoneId\":3,\"anonymous\":true,\"opensAt\":\"2026-10-08T23:59:59\",\"closesAt\":\"2026-10-01T09:00:00\"}"
    })
    @DisplayName("유효하지 않은 상호평가 양식 생성 요청은 400을 응답한다")
    void rejectInvalidRequest(String body) throws Exception {
        mockMvc.perform(post(URL, 2L).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

        then(facade).shouldHaveNoInteractions();
    }
}
