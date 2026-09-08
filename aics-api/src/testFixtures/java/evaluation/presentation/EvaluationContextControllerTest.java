package evaluation.presentation;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import kgu.developers.api.evaluation.application.PeerEvaluationFacade;
import kgu.developers.api.evaluation.presentation.EvaluationContextControllerImpl;
import kgu.developers.api.evaluation.presentation.response.EvaluationContextResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class EvaluationContextControllerTest {
    private static final Long SECTION_ID = 2L;
    private static final String USER_ID = "20260001";

    @Mock
    private PeerEvaluationFacade facade;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new EvaluationContextControllerImpl(facade)).build();
    }

    @Test
    @DisplayName("GET /sections/{sectionId}/evaluation-context는 평가 진입 ID를 반환한다")
    void getContext() throws Exception {
        given(facade.getContext(SECTION_ID, USER_ID)).willReturn(new EvaluationContextResponse("15", "3"));

        mockMvc.perform(get("/sections/{sectionId}/evaluation-context", SECTION_ID)
                .principal(new UsernamePasswordAuthenticationToken(USER_ID, null, List.of())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.presentationMilestoneId").value("15"))
            .andExpect(jsonPath("$.peerEvaluationFormId").value("3"));

        verify(facade).getContext(SECTION_ID, USER_ID);
    }
}
