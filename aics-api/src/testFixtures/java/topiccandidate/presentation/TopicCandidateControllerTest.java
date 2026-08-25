package topiccandidate.presentation;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import kgu.developers.api.topiccandidate.application.TopicCandidateFacade;
import kgu.developers.api.topiccandidate.presentation.TopicCandidateControllerImpl;
import kgu.developers.api.topiccandidate.presentation.request.TopicCandidateCreateRequest;
import kgu.developers.api.topiccandidate.presentation.response.TopicCandidateListResponse;
import kgu.developers.api.topiccandidate.presentation.response.TopicCandidatePersistResponse;
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

@ExtendWith(MockitoExtension.class)
class TopicCandidateControllerTest {

    private static final Long TEAM_ID = 1L;
    private static final String USER_ID = "202412345";

    @Mock
    private TopicCandidateFacade topicCandidateFacade;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TopicCandidateControllerImpl(topicCandidateFacade)).build();
    }

    @Test
    @DisplayName("GET /teams/{teamId}/topic-candidates는 후보 목록을 반환한다")
    void getTopicCandidates_ReturnsCandidateList() throws Exception {
        // given
        given(topicCandidateFacade.getTopicCandidates(TEAM_ID, USER_ID)).willReturn(response());

        // when & then
        mockMvc.perform(get("/teams/{teamId}/topic-candidates", TEAM_ID)
                .principal(new UsernamePasswordAuthenticationToken(USER_ID, null, List.of())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.contents[0].id").value(1L))
            .andExpect(jsonPath("$.contents[0].title").value("AI 기반 학습 도우미"))
            .andExpect(jsonPath("$.contents[0].proposerUserId").value(USER_ID))
            .andExpect(jsonPath("$.contents[0].description").value("학생별 맞춤형 학습 계획을 지원합니다."))
            .andExpect(jsonPath("$.contents[0].voteCount").value(3))
            .andExpect(jsonPath("$.contents[0].votedByMe").value(true));

        verify(topicCandidateFacade).getTopicCandidates(TEAM_ID, USER_ID);
    }

    @Test
    @DisplayName("POST /teams/{teamId}/topic-candidates는 후보를 등록하고 201을 반환한다")
    void createTopicCandidate_CreatesCandidate() throws Exception {
        // given
        TopicCandidateCreateRequest request = new TopicCandidateCreateRequest("AI 기반 학습 도우미", "학생별 맞춤형 학습 계획을 지원합니다.");
        given(topicCandidateFacade.createTopicCandidate(TEAM_ID, USER_ID, request)).willReturn(persistResponse());

        // when & then
        mockMvc.perform(post("/teams/{teamId}/topic-candidates", TEAM_ID)
                .principal(new UsernamePasswordAuthenticationToken(USER_ID, null, List.of()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"AI 기반 학습 도우미","description":"학생별 맞춤형 학습 계획을 지원합니다."}
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1L))
            .andExpect(jsonPath("$.proposerUserId").value(USER_ID))
            .andExpect(jsonPath("$.title").value("AI 기반 학습 도우미"))
            .andExpect(jsonPath("$.description").value("학생별 맞춤형 학습 계획을 지원합니다."));

        verify(topicCandidateFacade).createTopicCandidate(TEAM_ID, USER_ID, request);
    }

    private TopicCandidateListResponse response() {
        return new TopicCandidateListResponse(List.of(
            TopicCandidateListResponse.TopicCandidateResponse.builder()
                .id(1L)
                .title("AI 기반 학습 도우미")
                .proposerUserId(USER_ID)
                .description("학생별 맞춤형 학습 계획을 지원합니다.")
                .voteCount(3)
                .votedByMe(true)
                .build()
        ));
    }

    private TopicCandidatePersistResponse persistResponse() {
        return TopicCandidatePersistResponse.builder()
            .id(1L)
            .proposerUserId(USER_ID)
            .title("AI 기반 학습 도우미")
            .description("학생별 맞춤형 학습 계획을 지원합니다.")
            .build();
    }
}
