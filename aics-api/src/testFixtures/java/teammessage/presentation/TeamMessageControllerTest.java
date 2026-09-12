package teammessage.presentation;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import kgu.developers.api.teammessage.application.TeamMessageFacade;
import kgu.developers.api.teammessage.presentation.TeamMessageControllerImpl;
import org.junit.jupiter.api.BeforeEach;
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
class TeamMessageControllerTest {
    @Mock private TeamMessageFacade facade;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new TeamMessageControllerImpl(facade))
            .setValidator(validator)
            .build();
    }

    @Test
    void rejectsProposalFeedbackWithoutRelatedId() throws Exception {
        mockMvc.perform(post("/api/v1/teams/{teamId}/messages", 1L)
                .principal(new UsernamePasswordAuthenticationToken("202600001", null))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"relatedType\":\"PROPOSAL\",\"message\":\"피드백\"}"))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(facade);
    }

    @Test
    void rejectsMidReportFeedbackWithNonPositiveRelatedId() throws Exception {
        mockMvc.perform(post("/api/v1/teams/{teamId}/messages", 1L)
                .principal(new UsernamePasswordAuthenticationToken("202600001", null))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"relatedType\":\"MID_REPORT\",\"relatedId\":0,\"message\":\"피드백\"}"))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(facade);
    }
}
