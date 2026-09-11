package evaluation.presentation;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;
import kgu.developers.api.evaluation.application.TeamEvaluationFacade;
import kgu.developers.api.evaluation.presentation.TeamEvaluationControllerImpl;
import kgu.developers.api.evaluation.presentation.TeamEvaluationWindowState;
import kgu.developers.api.evaluation.presentation.request.TeamEvaluationScoreRequest;
import kgu.developers.api.evaluation.presentation.request.TeamEvaluationSubmitRequest;
import kgu.developers.api.evaluation.presentation.response.MyTeamEvaluationsResponse;
import kgu.developers.api.evaluation.presentation.response.TeamEvaluationCriterionResponse;
import kgu.developers.api.evaluation.presentation.response.TeamEvaluationResponse;
import kgu.developers.api.evaluation.presentation.response.TeamEvaluationScoreResponse;
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
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

@ExtendWith(MockitoExtension.class)
class TeamEvaluationControllerTest {
    private static final Long MILESTONE_ID = 1L;
    private static final Long TEAM_ID = 2L;
    private static final String USER_ID = "20260001";

    @Mock private TeamEvaluationFacade facade;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new TeamEvaluationControllerImpl(facade))
                .setValidator(validator)
                .build();
    }

    @Test
    @DisplayName("GET /milestones/{milestoneId}/team-evaluations/me는 내 발표 평가 현황을 반환한다")
    void getMyEvaluations() throws Exception {
        given(facade.getMyEvaluations(MILESTONE_ID, USER_ID)).willReturn(new MyTeamEvaluationsResponse(
                MILESTONE_ID,
                TeamEvaluationWindowState.OPEN,
                LocalDateTime.of(2026, 9, 11, 10, 0),
                LocalDateTime.of(2026, 9, 11, 18, 0),
                List.of(new TeamEvaluationCriterionResponse(10L, "발표 완성도", 10, 1)),
                List.of()
        ));

        mockMvc.perform(get("/milestones/{milestoneId}/team-evaluations/me", MILESTONE_ID)
                        .principal(new UsernamePasswordAuthenticationToken(USER_ID, null, List.of())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.milestoneId").value(MILESTONE_ID))
                .andExpect(jsonPath("$.windowState").value("OPEN"))
                .andExpect(jsonPath("$.criteria[0].id").value(10L));
    }

    @Test
    @DisplayName("PUT /milestones/{milestoneId}/team-evaluations/{teamId}는 팀 발표 평가를 저장한다")
    void submit() throws Exception {
        TeamEvaluationSubmitRequest request = new TeamEvaluationSubmitRequest(
                List.of(new TeamEvaluationScoreRequest(10L, 8))
        );
        given(facade.submit(MILESTONE_ID, TEAM_ID, USER_ID, request)).willReturn(
                new TeamEvaluationResponse(
                        20L, TEAM_ID, LocalDateTime.of(2026, 9, 11, 12, 0),
                        List.of(new TeamEvaluationScoreResponse(10L, 8))
                )
        );

        mockMvc.perform(put("/milestones/{milestoneId}/team-evaluations/{teamId}",
                        MILESTONE_ID, TEAM_ID)
                        .principal(new UsernamePasswordAuthenticationToken(USER_ID, null, List.of()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "scores": [{"criterionId": 10, "score": 8}]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(20L))
                .andExpect(jsonPath("$.teamId").value(TEAM_ID))
                .andExpect(jsonPath("$.scores[0].score").value(8));

        verify(facade).submit(MILESTONE_ID, TEAM_ID, USER_ID, request);
    }

    @Test
    @DisplayName("발표 평가 점수 목록이 비어 있으면 400을 반환한다")
    void rejectsEmptyScores() throws Exception {
        mockMvc.perform(put("/milestones/{milestoneId}/team-evaluations/{teamId}",
                        MILESTONE_ID, TEAM_ID)
                        .principal(new UsernamePasswordAuthenticationToken(USER_ID, null, List.of()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"scores\":[]}"))
                .andExpect(status().isBadRequest());
    }
}
