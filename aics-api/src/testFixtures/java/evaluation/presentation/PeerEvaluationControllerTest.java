package evaluation.presentation;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;
import kgu.developers.api.evaluation.application.PeerEvaluationFacade;
import kgu.developers.api.evaluation.presentation.PeerEvaluationAnswerKind;
import kgu.developers.api.evaluation.presentation.PeerEvaluationControllerImpl;
import kgu.developers.api.evaluation.presentation.request.PeerEvaluationAnswerRequest;
import kgu.developers.api.evaluation.presentation.request.PeerEvaluationResponseRequest;
import kgu.developers.api.evaluation.presentation.response.MyPeerEvaluationResponse;
import kgu.developers.api.evaluation.presentation.response.PeerEvaluationTargetResponse;
import kgu.developers.api.evaluation.presentation.response.PeerEvaluationTargetsResponse;
import kgu.developers.domain.evaluation.domain.PeerEvaluationSubmissionStatus;
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
class PeerEvaluationControllerTest {
    private static final Long FORM_ID = 1L;
    private static final String USER_ID = "20260001";

    @Mock
    private PeerEvaluationFacade facade;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new PeerEvaluationControllerImpl(facade))
            .setValidator(validator)
            .build();
    }

    @Test
    @DisplayName("GET /peer-evaluation-forms/{formId}/targets는 평가 대상과 기간 상태를 반환한다")
    void getTargets() throws Exception {
        given(facade.getTargets(FORM_ID, USER_ID)).willReturn(PeerEvaluationTargetsResponse.builder()
            .formId(FORM_ID)
            .title("상호평가")
            .windowState("OPEN")
            .windowMessage("평가 기간입니다.")
            .targets(List.of(new PeerEvaluationTargetResponse("20260002", "학생 B", "개발")))
            .build());

        mockMvc.perform(get("/peer-evaluation-forms/{formId}/targets", FORM_ID)
                .principal(new UsernamePasswordAuthenticationToken(USER_ID, null, List.of())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.formId").value(FORM_ID))
            .andExpect(jsonPath("$.windowState").value("OPEN"))
            .andExpect(jsonPath("$.targets[0].userId").value("20260002"));

        verify(facade).getTargets(FORM_ID, USER_ID);
    }

    @Test
    @DisplayName("POST /peer-evaluation-forms/{formId}/responses는 상호평가 응답을 저장한다")
    void submitResponse() throws Exception {
        PeerEvaluationResponseRequest request = new PeerEvaluationResponseRequest(
            "백엔드 구현", "프로젝트 평가",
            List.of(new PeerEvaluationAnswerRequest(
                PeerEvaluationAnswerKind.TEAMMATE_CONTRIBUTION, "20260002", 100, "기여 내용", "팀원 평가", null
            )), false
        );
        given(facade.submitResponse(FORM_ID, USER_ID, request)).willReturn(new MyPeerEvaluationResponse(
            10L, "백엔드 구현", "프로젝트 평가", List.of(),
            PeerEvaluationSubmissionStatus.DRAFT, LocalDateTime.of(2026, 9, 8, 12, 0), null
        ));

        mockMvc.perform(post("/peer-evaluation-forms/{formId}/responses", FORM_ID)
                .principal(new UsernamePasswordAuthenticationToken(USER_ID, null, List.of()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "selfContribution": "백엔드 구현",
                      "projectReviewComment": "프로젝트 평가",
                      "answers": [{
                        "kind": "TEAMMATE_CONTRIBUTION",
                        "targetUserId": "20260002",
                        "contributionPercent": 100,
                        "contributionDetail": "기여 내용",
                        "teammateAssessment": "팀원 평가"
                      }],
                      "submit": false
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(10L))
            .andExpect(jsonPath("$.status").value("DRAFT"));

        verify(facade).submitResponse(FORM_ID, USER_ID, request);
    }

    @Test
    @DisplayName("상호평가 answers가 누락되면 400을 반환한다")
    void submitResponseWithoutAnswers() throws Exception {
        mockMvc.perform(post("/peer-evaluation-forms/{formId}/responses", FORM_ID)
                .principal(new UsernamePasswordAuthenticationToken(USER_ID, null, List.of()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"submit\":false}"))
            .andExpect(status().isBadRequest());
    }
}
