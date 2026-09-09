package meetingrecord.presentation;

import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import kgu.developers.api.meetingrecord.application.MeetingActionFacade;
import kgu.developers.api.meetingrecord.presentation.MeetingActionControllerImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class MeetingActionControllerTest {

    private static final String USER_ID = "202412345";
    private static final Long ACTION_ID = 1L;

    @Mock
    private MeetingActionFacade meetingActionFacade;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new MeetingActionControllerImpl(meetingActionFacade)).build();
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(USER_ID, null)
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("DELETE /meeting-actions/{id}는 액션플랜을 삭제하고 204를 반환한다")
    void deleteMeetingAction_ReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/meeting-actions/{id}", ACTION_ID)
                .principal(new UsernamePasswordAuthenticationToken(USER_ID, null)))
            .andExpect(status().isNoContent());

        then(meetingActionFacade).should().deleteMeetingAction(ACTION_ID, USER_ID);
    }
}
