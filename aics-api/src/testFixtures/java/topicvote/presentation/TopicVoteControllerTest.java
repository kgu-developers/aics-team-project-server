package topicvote.presentation;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import kgu.developers.api.topicvote.application.TopicVoteFacade;
import kgu.developers.api.topicvote.presentation.TopicVoteControllerImpl;
import kgu.developers.api.topicvote.presentation.response.TopicVotePersistResponse;
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
class TopicVoteControllerTest {

    private static final Long CANDIDATE_ID = 1L;
    private static final String VOTER_USER_ID = "202412345";

    @Mock
    private TopicVoteFacade topicVoteFacade;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TopicVoteControllerImpl(topicVoteFacade)).build();
    }

    @Test
    @DisplayName("POST /topic-candidates/{candidateId}/vote는 투표를 등록하고 201을 반환한다")
    void vote_CreatesVote() throws Exception {
        // given
        given(topicVoteFacade.vote(CANDIDATE_ID, VOTER_USER_ID)).willReturn(
            new TopicVotePersistResponse(1L, CANDIDATE_ID, VOTER_USER_ID)
        );

        // when & then
        mockMvc.perform(post("/api/v1/topic-candidates/{candidateId}/vote", CANDIDATE_ID)
                .principal(new UsernamePasswordAuthenticationToken(VOTER_USER_ID, null, List.of())))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1L))
            .andExpect(jsonPath("$.candidateId").value(CANDIDATE_ID))
            .andExpect(jsonPath("$.voterUserId").value(VOTER_USER_ID));

        verify(topicVoteFacade).vote(CANDIDATE_ID, VOTER_USER_ID);
    }

    @Test
    @DisplayName("DELETE /topic-candidates/{candidateId}/vote는 투표를 취소하고 204를 반환한다")
    void cancelVote_CancelsVote() throws Exception {
        // when & then
        mockMvc.perform(delete("/api/v1/topic-candidates/{candidateId}/vote", CANDIDATE_ID)
                .principal(new UsernamePasswordAuthenticationToken(VOTER_USER_ID, null, List.of())))
            .andExpect(status().isNoContent());

        verify(topicVoteFacade).cancelVote(CANDIDATE_ID, VOTER_USER_ID);
    }
}
