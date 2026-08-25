package project.presentation;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import kgu.developers.api.project.application.ProjectFacade;
import kgu.developers.api.project.presentation.ProjectControllerImpl;
import kgu.developers.api.project.presentation.request.ProjectRequest;
import kgu.developers.api.project.presentation.response.ProjectResponse;
import kgu.developers.domain.project.domain.ApprovalStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class ProjectControllerTest {

    private static final Long TEAM_ID = 1L;
    private static final String USER_ID = "202412345";

    @Mock
    private ProjectFacade projectFacade;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ProjectControllerImpl(projectFacade)).build();
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(USER_ID, null)
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("GET /teams/{teamId}/project는 프로젝트 제안서를 반환한다")
    void getProject() throws Exception {
        given(projectFacade.getProject(TEAM_ID, USER_ID)).willReturn(response());

        mockMvc.perform(get("/teams/{teamId}/project", TEAM_ID)
                .principal(new UsernamePasswordAuthenticationToken(USER_ID, null)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(10L))
            .andExpect(jsonPath("$.teamId").value(TEAM_ID))
            .andExpect(jsonPath("$.title").value("AI 학습 도우미"))
            .andExpect(jsonPath("$.approvalStatus").value("DRAFT"));

        then(projectFacade).should().getProject(TEAM_ID, USER_ID);
    }

    @Test
    @DisplayName("PUT /teams/{teamId}/project는 요청 값을 제안서 등록에 전달한다")
    void saveProject() throws Exception {
        ProjectRequest request = new ProjectRequest(
            "AI 학습 도우미",
            "학습 기록을 분석하는 서비스",
            "개인별 피드백 자동화",
            "매주 월요일 대면 회의",
            "https://github.com/kgu/project",
            objectMapper.readTree("[{\"name\":\"Figma\",\"url\":\"https://figma.com/design\"}]")
        );
        given(projectFacade.saveProject(eq(TEAM_ID), eq(USER_ID), org.mockito.ArgumentMatchers.any(ProjectRequest.class)))
            .willReturn(response());

        mockMvc.perform(put("/teams/{teamId}/project", TEAM_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .principal(new UsernamePasswordAuthenticationToken(USER_ID, null)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("AI 학습 도우미"));

        ArgumentCaptor<ProjectRequest> requestCaptor = ArgumentCaptor.forClass(ProjectRequest.class);
        then(projectFacade).should().saveProject(eq(TEAM_ID), eq(USER_ID), requestCaptor.capture());
        org.assertj.core.api.Assertions.assertThat(requestCaptor.getValue())
            .extracting(ProjectRequest::title, ProjectRequest::description, ProjectRequest::goal)
            .containsExactly("AI 학습 도우미", "학습 기록을 분석하는 서비스", "개인별 피드백 자동화");
        org.assertj.core.api.Assertions.assertThat(requestCaptor.getValue().externalLinks())
            .isEqualTo(request.externalLinks());
    }

    @Test
    @DisplayName("PATCH /projects/{projectId}/proposal-complete는 완료 처리를 요청한다")
    void completeProposal() throws Exception {
        mockMvc.perform(patch("/projects/{projectId}/proposal-complete", 10L)
                .principal(new UsernamePasswordAuthenticationToken(USER_ID, null)))
            .andExpect(status().isOk());

        then(projectFacade).should().completeProposal(10L, USER_ID);
    }

    private ProjectResponse response() throws Exception {
        return ProjectResponse.builder()
            .id(10L)
            .teamId(TEAM_ID)
            .title("AI 학습 도우미")
            .description("학습 기록을 분석하는 서비스")
            .goal("개인별 피드백 자동화")
            .meetingStyle("매주 월요일 대면 회의")
            .repositoryUrl("https://github.com/kgu/project")
            .externalLinks(objectMapper.readTree("[]"))
            .approvalStatus(ApprovalStatus.DRAFT)
            .build();
    }
}
