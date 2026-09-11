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
import kgu.developers.admin.evaluation.application.PeerEvaluationAdminFacade;
import kgu.developers.admin.evaluation.presentation.PeerEvaluationAdminControllerImpl;
import kgu.developers.admin.evaluation.presentation.response.PeerEvaluationAdminListResponse;
import kgu.developers.admin.evaluation.presentation.response.PeerEvaluationAdminTeamDetailResponse;
import kgu.developers.admin.evaluation.presentation.response.PeerEvaluationAdminTeamSummaryResponse;
import kgu.developers.admin.evaluation.presentation.response.PeerEvaluationMeetingRecordSummaryResponse;
import kgu.developers.admin.evaluation.presentation.response.PeerEvaluationMemberResponse;
import kgu.developers.admin.evaluation.presentation.response.PeerEvaluationRowResponse;
import kgu.developers.admin.evaluation.presentation.response.PeerEvaluationScoreDetailResponse;
import kgu.developers.common.config.CorsConfig;
import kgu.developers.common.exception.GlobalExceptionHandler;
import kgu.developers.domain.evaluation.domain.PeerEvaluationSubmissionStatus;
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
    PeerEvaluationAdminControllerImpl.class,
    GlobalExceptionHandler.class
})
@TestPropertySource(properties = {
    "jwt.secret_key=local-dev-jwt-secret-key-0123456789",
    "jwt.issuer=kgudevelopers@gmail.com",
    "cors.allowed-origins=http://localhost:5173",
    "spring.security.user.name=admin",
    "spring.security.user.password=admin"
})
class PeerEvaluationAdminControllerTest {

    private static final String BASE_URL = "/api/v1/admin/oop/sections/{sectionId}/peer-evaluations";

    @SpringBootConfiguration
    static class TestApp {
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PeerEvaluationAdminFacade facade;

    @MockitoBean
    private TokenRevocationStore tokenRevocationStore;

    @Test
    @DisplayName("미인증 사용자는 상호평가 결과 조회 API에 접근할 수 없다")
    void unauthenticated() throws Exception {
        mockMvc.perform(get(BASE_URL, 1L))
            .andExpect(status().isUnauthorized());

        then(facade).shouldHaveNoInteractions();
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("일반 사용자는 상호평가 결과 조회 API에 접근할 수 없다")
    void userForbidden() throws Exception {
        mockMvc.perform(get(BASE_URL, 1L))
            .andExpect(status().isForbidden());

        then(facade).shouldHaveNoInteractions();
    }

    @Test
    @WithMockUser(username = "202012345", roles = "ADMIN")
    @DisplayName("관리자는 분반 상호평가 팀 목록 및 요약을 정상 조회한다")
    void getPeerEvaluations_Success() throws Exception {
        PeerEvaluationAdminTeamSummaryResponse teamSummary = PeerEvaluationAdminTeamSummaryResponse.builder()
            .teamId(10L)
            .teamName("OOP-01 - 1팀")
            .submittedCount(1)
            .totalMemberCount(2)
            .lastSubmittedAt(LocalDateTime.of(2026, 12, 14, 15, 30))
            .meetingRecordCount(3L)
            .build();
        PeerEvaluationAdminListResponse response = PeerEvaluationAdminListResponse.builder()
            .sectionId(1L)
            .formId(100L)
            .opensAt(LocalDateTime.of(2026, 12, 1, 0, 0))
            .closesAt(LocalDateTime.of(2026, 12, 15, 23, 59))
            .teams(List.of(teamSummary))
            .build();
        given(facade.getPeerEvaluations(1L, null, "202012345")).willReturn(response);

        mockMvc.perform(get(BASE_URL, 1L))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sectionId").value(1L))
            .andExpect(jsonPath("$.formId").value(100L))
            .andExpect(jsonPath("$.teams[0].teamId").value(10L))
            .andExpect(jsonPath("$.teams[0].teamName").value("OOP-01 - 1팀"))
            .andExpect(jsonPath("$.teams[0].submittedCount").value(1))
            .andExpect(jsonPath("$.teams[0].totalMemberCount").value(2))
            .andExpect(jsonPath("$.teams[0].meetingRecordCount").value(3));
    }

    @Test
    @WithMockUser(username = "202012345", roles = "ADMIN")
    @DisplayName("관리자는 팀 상호평가 결과 상세를 정상 조회한다")
    void getTeamPeerEvaluationDetail_Success() throws Exception {
        PeerEvaluationMemberResponse member = PeerEvaluationMemberResponse.builder()
            .userId("20260001")
            .name("김민준")
            .isLeader(true)
            .role("팀장")
            .averageReceivedScore(30.0)
            .build();

        PeerEvaluationRowResponse row = PeerEvaluationRowResponse.builder()
            .evaluatorId("20260001")
            .evaluatorName("김민준")
            .isLeader(true)
            .status(PeerEvaluationSubmissionStatus.SUBMITTED)
            .averageScore(30.0)
            .scores(List.of(
                PeerEvaluationScoreDetailResponse.self("20260001", "김민준"),
                PeerEvaluationScoreDetailResponse.score("20260002", "이서연", 30)
            ))
            .selfContribution("기여 내용")
            .projectReviewComment("총평")
            .reflectionComment("회고")
            .teammateAssessments(List.of())
            .build();

        PeerEvaluationMeetingRecordSummaryResponse meeting = PeerEvaluationMeetingRecordSummaryResponse.builder()
            .id(1L)
            .title("킥오프")
            .phase(MeetingPhase.MID_CHECK)
            .meetingAt(LocalDateTime.of(2026, 10, 1, 14, 0))
            .participantCount(2)
            .build();

        PeerEvaluationAdminTeamDetailResponse response = PeerEvaluationAdminTeamDetailResponse.builder()
            .teamId(10L)
            .teamName("OOP-01 - 1팀")
            .formId(100L)
            .closesAt(LocalDateTime.of(2026, 12, 15, 23, 59))
            .members(List.of(member))
            .evaluations(List.of(row))
            .meetingRecords(List.of(meeting))
            .build();

        given(facade.getTeamPeerEvaluationDetail(1L, 10L, null, "202012345")).willReturn(response);

        mockMvc.perform(get(BASE_URL + "/teams/{teamId}", 1L, 10L))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.teamId").value(10L))
            .andExpect(jsonPath("$.teamName").value("OOP-01 - 1팀"))
            .andExpect(jsonPath("$.members[0].userId").value("20260001"))
            .andExpect(jsonPath("$.members[0].name").value("김민준"))
            .andExpect(jsonPath("$.evaluations[0].evaluatorId").value("20260001"))
            .andExpect(jsonPath("$.evaluations[0].status").value("SUBMITTED"))
            .andExpect(jsonPath("$.evaluations[0].scores[0].isSelf").value(true))
            .andExpect(jsonPath("$.evaluations[0].scores[1].contributionPercent").value(30))
            .andExpect(jsonPath("$.meetingRecords[0].title").value("킥오프"));
    }

    @Test
    @WithMockUser(username = "202012345", roles = "ADMIN")
    @DisplayName("담당 교수가 아닌 관리자의 조회 요청은 403을 응답한다")
    void anotherProfessorForbidden() throws Exception {
        willThrow(new AccessDeniedException("담당 분반만 접근할 수 있습니다."))
            .given(facade)
            .getPeerEvaluations(1L, null, "202012345");

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
