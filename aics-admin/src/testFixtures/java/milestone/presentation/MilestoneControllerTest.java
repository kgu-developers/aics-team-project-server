package milestone.presentation;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import kgu.developers.admin.config.JwtCookieAuthenticationFilter;
import kgu.developers.admin.config.SecurityConfig;
import kgu.developers.admin.milestone.application.MilestoneFacade;
import kgu.developers.admin.milestone.presentation.MilestoneControllerImpl;
import kgu.developers.admin.milestone.presentation.response.MilestoneListResponse;
import kgu.developers.admin.milestone.presentation.response.MilestonePersistResponse;
import kgu.developers.common.config.CorsConfig;
import kgu.developers.common.exception.GlobalExceptionHandler;
import kgu.developers.domain.milestone.exception.MilestoneNotFoundException;
import kgu.developers.domain.milestone.exception.MilestoneSectionMismatchException;
import kgu.developers.globalutils.jwt.JwtUtil;

@WebMvcTest
@Import({
        SecurityConfig.class,
        JwtCookieAuthenticationFilter.class,
        JwtUtil.class,
        CorsConfig.class,
        GlobalExceptionHandler.class,
        MilestoneControllerImpl.class
})
@TestPropertySource(properties = {
        "jwt.secret_key=local-dev-jwt-secret-key-0123456789",
        "jwt.issuer=kgudevelopers@gmail.com",
        "cors.allowed-origins=http://localhost:5173",
        "spring.security.user.name=admin",
        "spring.security.user.password=admin"
})
class MilestoneControllerTest {
    private static final String MILESTONES_URL = "/api/v1/admin/oop/sections/1/milestones";

    @SpringBootConfiguration
    static class TestApp {
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MilestoneFacade milestoneFacade;

    @Test
    @DisplayName("미인증 사용자는 마일스톤 목록을 조회할 수 없다")
    void unauthenticated() throws Exception {
        mockMvc.perform(get(MILESTONES_URL))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    @DisplayName("학생은 관리자 마일스톤 API에 접근할 수 없다")
    void studentForbidden() throws Exception {
        mockMvc.perform(get(MILESTONES_URL))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "PROFESSOR")
    @DisplayName("교수자는 분반별 마일스톤 목록을 조회할 수 있다")
    void professorCanGetMilestones() throws Exception {
        given(milestoneFacade.getMilestones(1L, null))
                .willReturn(new MilestoneListResponse(List.of()));

        mockMvc.perform(get(MILESTONES_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contents").isArray());
    }

    @Test
    @WithMockUser(roles = "PROFESSOR")
    @DisplayName("교수자는 올바른 요청으로 마일스톤을 생성할 수 있다")
    void createMilestone() throws Exception {
        given(milestoneFacade.createMilestone(org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.any()))
                .willReturn(MilestonePersistResponse.of(10L));

        mockMvc.perform(post(MILESTONES_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "제안서",
                                  "description": "제안서 제출",
                                  "weekNumber": 2,
                                  "schedule": {
                                    "dueAt": "2026-09-10T23:59:59",
                                    "revisionUntil": "2026-09-12T23:59:59"
                                  }
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10));
    }

    @Test
    @WithMockUser(roles = "PROFESSOR")
    @DisplayName("교수자는 마일스톤 내용과 일정을 수정할 수 있다")
    void updateMilestone() throws Exception {
        mockMvc.perform(put(MILESTONES_URL + "/2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "중간보고서",
                                  "description": "중간 진행상황 제출",
                                  "schedule": {
                                    "dueAt": "2026-10-10T23:59:59",
                                    "revisionUntil": "2026-10-12T23:59:59"
                                  }
                                }
                                """))
                .andExpect(status().isNoContent());

        verify(milestoneFacade).updateMilestone(
                org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.eq(2L),
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    @WithMockUser(roles = "PROFESSOR")
    @DisplayName("교수자는 마일스톤 공개 상태를 변경할 수 있다")
    void changeStatus() throws Exception {
        mockMvc.perform(patch(MILESTONES_URL + "/2/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "PUBLISHED"}
                                """))
                .andExpect(status().isNoContent());

        verify(milestoneFacade).changeStatus(
                org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.eq(2L),
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    @WithMockUser(roles = "PROFESSOR")
    @DisplayName("교수자는 마일스톤 주차를 일괄 변경할 수 있다")
    void updateWeekNumbers() throws Exception {
        mockMvc.perform(put(MILESTONES_URL + "/week-numbers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "changes": [
                                    {"milestoneId": 2, "weekNumber": 3},
                                    {"milestoneId": 3, "weekNumber": 4}
                                  ]
                                }
                                """))
                .andExpect(status().isNoContent());

        verify(milestoneFacade).updateWeekNumbers(
                org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    @WithMockUser(roles = "PROFESSOR")
    @DisplayName("필수 값이 없는 생성 요청은 400을 응답한다")
    void invalidCreateRequest() throws Exception {
        mockMvc.perform(post(MILESTONES_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "",
                                  "weekNumber": 0,
                                  "schedule": {}
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    @WithMockUser(roles = "PROFESSOR")
    @DisplayName("0 이하의 분반 식별자는 400을 응답한다")
    void invalidSectionId() throws Exception {
        mockMvc.perform(get("/api/v1/admin/oop/sections/0/milestones"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    @WithMockUser(roles = "PROFESSOR")
    @DisplayName("0 이하의 마일스톤 식별자는 400을 응답한다")
    void invalidMilestoneId() throws Exception {
        mockMvc.perform(get(MILESTONES_URL + "/0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    @WithMockUser(roles = "PROFESSOR")
    @DisplayName("다른 분반의 마일스톤 상세 요청은 403을 응답한다")
    void anotherSectionForbidden() throws Exception {
        given(milestoneFacade.getMilestone(1L, 2L))
                .willThrow(new MilestoneSectionMismatchException(2L, 1L));

        mockMvc.perform(get(MILESTONES_URL + "/2"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("MILESTONE_SECTION_FORBIDDEN"));
    }

    @Test
    @WithMockUser(roles = "PROFESSOR")
    @DisplayName("존재하지 않는 마일스톤 상세 요청은 404를 응답한다")
    void milestoneNotFound() throws Exception {
        given(milestoneFacade.getMilestone(1L, 404L))
                .willThrow(new MilestoneNotFoundException(404L));

        mockMvc.perform(get(MILESTONES_URL + "/404"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("MILESTONE_NOT_FOUND"));
    }
}
