package project.presentation;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import kgu.developers.api.project.application.ProjectFacade;
import kgu.developers.api.project.presentation.ProjectControllerImpl;
import kgu.developers.api.project.presentation.request.ProjectRequest;
import kgu.developers.api.project.presentation.response.ProjectResponse;
import kgu.developers.api.project.presentation.response.ProjectApprovalSummaryResponse;
import kgu.developers.domain.project.domain.ApprovalStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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
    @DisplayName("GET /api/v1/teams/{teamId}/project는 프로젝트 제안서를 반환한다")
    void getProject() throws Exception {
        given(projectFacade.getProject(TEAM_ID, USER_ID)).willReturn(response());

        mockMvc.perform(get("/api/v1/teams/{teamId}/project", TEAM_ID)
                .principal(new UsernamePasswordAuthenticationToken(USER_ID, null)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(10L))
            .andExpect(jsonPath("$.teamId").value(TEAM_ID))
            .andExpect(jsonPath("$.title").value("AI 학습 도우미"))
            .andExpect(jsonPath("$.approvalStatus").value("DRAFT"));

        then(projectFacade).should().getProject(TEAM_ID, USER_ID);
    }

    @Test
    @DisplayName("PUT /api/v1/teams/{teamId}/project는 요청 값을 제안서 등록에 전달한다")
    void saveProject() throws Exception {
        ProjectRequest request = new ProjectRequest(
            "AI 학습 도우미",
            "학습 기록을 분석하는 서비스",
            "개인별 피드백 자동화",
            "종류: 학습 로그, 개수: 약 1만 건, 수집: 자체 수집",
            objectMapper.readTree("[{\"title\":\"홈\",\"description\":\"요약\",\"imageFileId\":1}]"),
            "매주 월요일 대면 회의",
            "https://github.com/kgu/project",
            objectMapper.readTree("[{\"name\":\"Figma\",\"url\":\"https://figma.com/design\"}]")
        );
        given(projectFacade.saveProject(eq(TEAM_ID), eq(USER_ID), org.mockito.ArgumentMatchers.any(ProjectRequest.class)))
            .willReturn(response());

        mockMvc.perform(put("/api/v1/teams/{teamId}/project", TEAM_ID)
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

    @ParameterizedTest(name = "{0} 길이 초과는 400을 반환한다")
    @CsvSource({"title, 201", "meetingStyle, 201", "repositoryUrl, 256"})
    @DisplayName("PUT /api/v1/teams/{teamId}/project는 DB 길이 제한 초과 시 400을 반환한다")
    void saveProjectRejectsTooLongField(String field, int length) throws Exception {
        String tooLong = "a".repeat(length);
        ProjectRequest request = new ProjectRequest(
            "title".equals(field) ? tooLong : "AI 학습 도우미",
            "학습 기록을 분석하는 서비스",
            "개인별 피드백 자동화",
            "종류: 학습 로그, 개수: 약 1만 건, 수집: 자체 수집",
            objectMapper.readTree("[]"),
            "meetingStyle".equals(field) ? tooLong : "매주 월요일 대면 회의",
            "repositoryUrl".equals(field) ? tooLong : "https://github.com/kgu/project",
            objectMapper.readTree("[]")
        );

        mockMvc.perform(put("/api/v1/teams/{teamId}/project", TEAM_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .principal(new UsernamePasswordAuthenticationToken(USER_ID, null)))
            .andExpect(status().isBadRequest());

        then(projectFacade).shouldHaveNoInteractions();
    }

    // screenConfiguration 컬럼은 NOT NULL이고 순서 있는 목록이라 배열이어야 한다. @NotNull만으로는
    // JSON `null`을 못 막는다 — Jackson이 NullNode로 역직렬화해서 jsonb에 `null`이 저장돼버린다.
    @ParameterizedTest(name = "screenConfiguration이 {0}이면 400을 반환한다")
    @CsvSource({"null", "'{\"title\":\"홈\"}'", "'\"문자열\"'", "123"})
    @DisplayName("PUT /api/v1/teams/{teamId}/project는 화면 구성이 배열이 아니면 400을 반환한다")
    void saveProjectRejectsNonArrayScreenConfiguration(String screenConfigurationJson) throws Exception {
        String body = """
            {"title":"AI 학습 도우미","description":"설명","goal":"목표",
             "dataConfiguration":"종류: 학습 로그","screenConfiguration":%s,
             "meetingStyle":"대면","repositoryUrl":"https://github.com/kgu/project","externalLinks":[]}
            """.formatted(screenConfigurationJson);

        mockMvc.perform(put("/api/v1/teams/{teamId}/project", TEAM_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .principal(new UsernamePasswordAuthenticationToken(USER_ID, null)))
            .andExpect(status().isBadRequest());

        then(projectFacade).shouldHaveNoInteractions();
    }

    // 파사드의 imageFileId 소유권 검사는 "각 원소가 객체이고 imageFileId가 정수"라는 전제 위에서
    // 도니까, 그 전제를 여기 입력 경계에서 400으로 끊어야 검사 없이 저장되는 구멍이 안 생긴다.
    @ParameterizedTest(name = "screenConfiguration 항목이 {0}이면 400을 반환한다")
    @CsvSource({"'\"홈\"'", "'{\"title\":\"홈\",\"imageFileId\":\"1\"}'", "'{\"title\":\"홈\",\"imageFileId\":1.5}'"})
    @DisplayName("PUT /api/v1/teams/{teamId}/project는 화면 항목 모양이 잘못되면 400을 반환한다")
    void saveProjectRejectsMalformedScreenItem(String screenJson) throws Exception {
        String body = """
            {"title":"AI 학습 도우미","description":"설명","goal":"목표",
             "dataConfiguration":"종류: 학습 로그","screenConfiguration":[%s],
             "meetingStyle":"대면","repositoryUrl":"https://github.com/kgu/project","externalLinks":[]}
            """.formatted(screenJson);

        mockMvc.perform(put("/api/v1/teams/{teamId}/project", TEAM_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .principal(new UsernamePasswordAuthenticationToken(USER_ID, null)))
            .andExpect(status().isBadRequest());

        then(projectFacade).shouldHaveNoInteractions();
    }

    // DB가 NOT NULL이라 null은 여기서 400으로 끊어야 500이 안 난다.
    @Test
    @DisplayName("PUT /api/v1/teams/{teamId}/project는 데이터 구성이 null이면 400을 반환한다")
    void saveProjectRejectsNullDataConfiguration() throws Exception {
        String body = """
            {"title":"AI 학습 도우미","description":"설명","goal":"목표",
             "dataConfiguration":null,"screenConfiguration":[],
             "meetingStyle":"대면","repositoryUrl":"https://github.com/kgu/project","externalLinks":[]}
            """;

        mockMvc.perform(put("/api/v1/teams/{teamId}/project", TEAM_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .principal(new UsernamePasswordAuthenticationToken(USER_ID, null)))
            .andExpect(status().isBadRequest());

        then(projectFacade).shouldHaveNoInteractions();
    }

    // 주제 확정이 데이터 구성을 빈 문자열로 만들어 두므로(ProjectCommandService.finalizeTopic),
    // 조회한 제안서를 그대로 다시 저장하는 것이 400이 나면 안 된다.
    @Test
    @DisplayName("PUT /api/v1/teams/{teamId}/project는 데이터 구성이 미입력(빈 문자열)이어도 저장한다")
    void saveProjectAcceptsEmptyDataConfiguration() throws Exception {
        given(projectFacade.saveProject(eq(TEAM_ID), eq(USER_ID), org.mockito.ArgumentMatchers.any(ProjectRequest.class)))
            .willReturn(response());
        String body = """
            {"title":"AI 학습 도우미","description":"설명","goal":"목표",
             "dataConfiguration":"","screenConfiguration":[],
             "meetingStyle":"대면","repositoryUrl":"https://github.com/kgu/project","externalLinks":[]}
            """;

        mockMvc.perform(put("/api/v1/teams/{teamId}/project", TEAM_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .principal(new UsernamePasswordAuthenticationToken(USER_ID, null)))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PATCH /api/v1/projects/{projectId}/proposal-complete는 완료 처리를 요청한다")
    void completeProposal() throws Exception {
        mockMvc.perform(patch("/api/v1/projects/{projectId}/proposal-complete", 10L)
                .principal(new UsernamePasswordAuthenticationToken(USER_ID, null)))
            .andExpect(status().isOk());

        then(projectFacade).should().completeProposal(10L, USER_ID);
    }

    @Test
    @DisplayName("POST /api/v1/projects/{projectId}/approval은 인증 사용자로 동의를 요청하고 200을 반환한다")
    void approveProject() throws Exception {
        mockMvc.perform(post("/api/v1/projects/{projectId}/approval", 10L)
                .principal(new UsernamePasswordAuthenticationToken(USER_ID, null)))
            .andExpect(status().isOk());

        then(projectFacade).should().approveProject(10L, USER_ID);
    }

    @Test
    @DisplayName("DELETE /api/v1/projects/{projectId}는 인증 사용자로 삭제를 요청하고 204를 반환한다")
    void deleteProject() throws Exception {
        mockMvc.perform(delete("/api/v1/projects/{projectId}", 10L)
                .principal(new UsernamePasswordAuthenticationToken(USER_ID, null)))
            .andExpect(status().isNoContent());

        then(projectFacade).should().deleteProject(10L, USER_ID);
    }

    @Test
    @DisplayName("GET /api/v1/projects/{projectId}/approvals는 팀원 동의 진행 현황을 반환한다")
    void getApprovalSummary() throws Exception {
        given(projectFacade.getApprovalSummary(10L, USER_ID))
            .willReturn(ProjectApprovalSummaryResponse.of(2, 4));

        mockMvc.perform(get("/api/v1/projects/{projectId}/approvals", 10L)
                .principal(new UsernamePasswordAuthenticationToken(USER_ID, null)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.approvedCount").value(2))
            .andExpect(jsonPath("$.totalCount").value(4))
            .andExpect(jsonPath("$.progress").value("2/4"));
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
