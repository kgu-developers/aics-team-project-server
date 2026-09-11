package evaluation.presentation;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;
import kgu.developers.admin.config.SecurityConfig;
import kgu.developers.admin.evaluation.application.PresentationEvaluationAdminFacade;
import kgu.developers.admin.evaluation.presentation.PresentationEvaluationAdminControllerImpl;
import kgu.developers.admin.evaluation.presentation.response.PresentationEvaluationAdminListResponse;
import kgu.developers.admin.evaluation.presentation.response.PresentationEvaluationAdminRowResponse;
import kgu.developers.admin.evaluation.presentation.response.PresentationEvaluationAdminTeamDetailResponse;
import kgu.developers.admin.evaluation.presentation.response.PresentationEvaluationAdminTeamSummaryResponse;
import kgu.developers.admin.evaluation.presentation.response.PresentationEvaluationCriterionScoreResponse;
import kgu.developers.admin.evaluation.presentation.response.PresentationEvaluationCriterionSimpleResponse;
import kgu.developers.admin.evaluation.presentation.response.PresentationMeetingRecordResponse;
import kgu.developers.common.config.CorsConfig;
import kgu.developers.common.exception.GlobalExceptionHandler;
import kgu.developers.domain.meetingrecord.domain.MeetingPhase;
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
    PresentationEvaluationAdminControllerImpl.class,
    GlobalExceptionHandler.class
})
@TestPropertySource(properties = {
    "jwt.secret_key=local-dev-jwt-secret-key-0123456789",
    "jwt.issuer=kgudevelopers@gmail.com",
    "cors.allowed-origins=http://localhost:5173",
    "spring.security.user.name=admin",
    "spring.security.user.password=admin"
})
class PresentationEvaluationAdminControllerTest {

    private static final String BASE_URL = "/api/v1/admin/oop/sections/{sectionId}/presentation-evaluations";

    @SpringBootConfiguration
    static class TestApp {
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PresentationEvaluationAdminFacade facade;

    @MockitoBean
    private TokenRevocationStore tokenRevocationStore;

    @Test
    @DisplayName("미인증 사용자는 발표 평가 결과 조회 API에 접근할 수 없다")
    void unauthenticated() throws Exception {
        mockMvc.perform(get(BASE_URL, 1L))
            .andExpect(status().isUnauthorized());

        then(facade).shouldHaveNoInteractions();
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("일반 사용자는 발표 평가 결과 조회 API에 접근할 수 없다")
    void userForbidden() throws Exception {
        mockMvc.perform(get(BASE_URL, 1L))
            .andExpect(status().isForbidden());

        then(facade).shouldHaveNoInteractions();
    }

    @Test
    @WithMockUser(username = "202012345", roles = "ADMIN")
    @DisplayName("관리자는 분반 발표 평가 현황 목록을 정상 조회한다")
    void getPresentationEvaluations_Success() throws Exception {
        PresentationEvaluationCriterionSimpleResponse criterion =
            PresentationEvaluationCriterionSimpleResponse.builder()
                .criterionId(1L)
                .title("완성도")
                .maxScore(50)
                .displayOrder(0)
                .build();

        PresentationEvaluationCriterionScoreResponse score =
            PresentationEvaluationCriterionScoreResponse.of(1L, "완성도", 45.0);

        PresentationEvaluationAdminTeamSummaryResponse teamSummary =
            PresentationEvaluationAdminTeamSummaryResponse.builder()
                .teamId(10L)
                .teamName("1팀")
                .projectTitle("AI 플랫폼")
                .evaluationCount(3)
                .scores(List.of(score))
                .totalScore(45.0)
                .build();

        PresentationEvaluationAdminListResponse response =
            PresentationEvaluationAdminListResponse.builder()
                .sectionId(1L)
                .milestoneId(100L)
                .milestoneTitle("최종 발표")
                .closesAt(LocalDateTime.of(2026, 12, 15, 23, 59))
                .criteria(List.of(criterion))
                .teams(List.of(teamSummary))
                .build();

        given(facade.getPresentationEvaluations(1L, null, "202012345")).willReturn(response);

        mockMvc.perform(get(BASE_URL, 1L))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sectionId").value(1L))
            .andExpect(jsonPath("$.milestoneId").value(100L))
            .andExpect(jsonPath("$.milestoneTitle").value("최종 발표"))
            .andExpect(jsonPath("$.criteria[0].title").value("완성도"))
            .andExpect(jsonPath("$.teams[0].teamId").value(10L))
            .andExpect(jsonPath("$.teams[0].teamName").value("1팀"))
            .andExpect(jsonPath("$.teams[0].projectTitle").value("AI 플랫폼"))
            .andExpect(jsonPath("$.teams[0].evaluationCount").value(3))
            .andExpect(jsonPath("$.teams[0].scores[0].score").value(45.0))
            .andExpect(jsonPath("$.teams[0].totalScore").value(45.0));
    }

    @Test
    @WithMockUser(username = "202012345", roles = "ADMIN")
    @DisplayName("관리자는 팀 발표 평가 결과 상세를 정상 조회한다")
    void getTeamPresentationEvaluationDetail_Success() throws Exception {
        PresentationEvaluationCriterionSimpleResponse criterion =
            PresentationEvaluationCriterionSimpleResponse.builder()
                .criterionId(1L)
                .title("완성도")
                .maxScore(50)
                .displayOrder(0)
                .build();

        PresentationEvaluationCriterionScoreResponse score =
            PresentationEvaluationCriterionScoreResponse.of(1L, "완성도", 45.0);

        PresentationEvaluationAdminRowResponse row =
            PresentationEvaluationAdminRowResponse.builder()
                .evaluatorId("20260002")
                .evaluatorName("이영희")
                .teamName("2팀")
                .isSubmitted(true)
                .submittedAt(LocalDateTime.of(2026, 12, 12, 15, 0))
                .scores(List.of(score))
                .totalScore(45)
                .build();

        PresentationMeetingRecordResponse meetingRecord =
            PresentationMeetingRecordResponse.builder()
                .id(501L)
                .title("발표 리허설")
                .phase(MeetingPhase.FINAL)
                .meetingAt(LocalDateTime.of(2026, 12, 8, 14, 0))
                .participantCount(4)
                .build();

        PresentationEvaluationAdminTeamDetailResponse response =
            PresentationEvaluationAdminTeamDetailResponse.builder()
                .teamId(10L)
                .teamName("1팀")
                .projectTitle("AI 플랫폼")
                .milestoneId(100L)
                .closesAt(LocalDateTime.of(2026, 12, 15, 23, 59))
                .criteria(List.of(criterion))
                .evaluations(List.of(row))
                .meetingRecords(List.of(meetingRecord))
                .build();

        given(facade.getTeamPresentationEvaluationDetail(1L, 10L, null, "202012345")).willReturn(response);

        mockMvc.perform(get(BASE_URL + "/teams/{teamId}", 1L, 10L))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.teamId").value(10L))
            .andExpect(jsonPath("$.teamName").value("1팀"))
            .andExpect(jsonPath("$.projectTitle").value("AI 플랫폼"))
            .andExpect(jsonPath("$.criteria[0].title").value("완성도"))
            .andExpect(jsonPath("$.evaluations[0].evaluatorId").value("20260002"))
            .andExpect(jsonPath("$.evaluations[0].evaluatorName").value("이영희"))
            .andExpect(jsonPath("$.evaluations[0].isSubmitted").value(true))
            .andExpect(jsonPath("$.evaluations[0].scores[0].score").value(45.0))
            .andExpect(jsonPath("$.evaluations[0].totalScore").value(45))
            .andExpect(jsonPath("$.meetingRecords[0].title").value("발표 리허설"));
    }

    @Test
    @WithMockUser(username = "202012345", roles = "ADMIN")
    @DisplayName("담당 교수가 아닌 관리자의 조회 요청은 403을 응답한다")
    void anotherProfessorForbidden() throws Exception {
        willThrow(new AccessDeniedException("담당 분반만 접근할 수 있습니다."))
            .given(facade)
            .getPresentationEvaluations(1L, null, "202012345");

        mockMvc.perform(get(BASE_URL, 1L))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @ParameterizedTest(name = "sectionId={0}")
    @ValueSource(longs = {0L, -1L})
    @WithMockUser(roles = "ADMIN")
    @DisplayName("0 이하 분반 id는 400을 응답한다")
    void rejectNonPositiveSectionId(long sectionId) throws Exception {
        mockMvc.perform(get(BASE_URL, sectionId))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_INPUT"));

        then(facade).shouldHaveNoInteractions();
    }

    @ParameterizedTest(name = "teamId={0}")
    @ValueSource(longs = {0L, -1L})
    @WithMockUser(roles = "ADMIN")
    @DisplayName("0 이하 팀 id는 400을 응답한다")
    void rejectNonPositiveTeamId(long teamId) throws Exception {
        mockMvc.perform(get(BASE_URL + "/teams/{teamId}", 1L, teamId))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_INPUT"));

        then(facade).shouldHaveNoInteractions();
    }
}
