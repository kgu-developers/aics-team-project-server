package evaluation.presentation;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import kgu.developers.admin.evaluation.application.TeamEvaluationCriterionFacade;
import kgu.developers.admin.config.SecurityConfig;
import kgu.developers.admin.evaluation.presentation.TeamEvaluationCriterionControllerImpl;
import kgu.developers.admin.evaluation.presentation.request.TeamEvaluationCriterionCreateRequest;
import kgu.developers.admin.evaluation.presentation.response.TeamEvaluationCriterionListResponse;
import kgu.developers.admin.evaluation.presentation.response.TeamEvaluationCriterionPersistResponse;
import kgu.developers.admin.evaluation.presentation.response.TeamEvaluationCriterionResponse;
import kgu.developers.common.exception.GlobalExceptionHandler;
import kgu.developers.common.config.CorsConfig;
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
import org.springframework.http.MediaType;
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
    TeamEvaluationCriterionControllerImpl.class,
    GlobalExceptionHandler.class
})
@TestPropertySource(properties = {
    "jwt.secret_key=local-dev-jwt-secret-key-0123456789",
    "jwt.issuer=kgudevelopers@gmail.com",
    "cors.allowed-origins=http://localhost:5173",
    "spring.security.user.name=admin",
    "spring.security.user.password=admin"
})
class TeamEvaluationCriterionControllerTest {

  private static final String URL =
      "/api/v1/admin/oop/sections/{sectionId}/team-evaluation-criteria";
  private static final String VALID_BODY =
      """
      {"title":"객체지향 설계","maxScore":30,"displayOrder":0}
      """;

  @SpringBootConfiguration
  static class TestApp {
  }

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private TeamEvaluationCriterionFacade facade;

  @MockitoBean
  private TokenRevocationStore tokenRevocationStore;

  @Test
  @DisplayName("미인증 사용자는 발표 평가 항목 API에 접근할 수 없다")
  void unauthenticated() throws Exception {
    mockMvc.perform(get(URL, 2L))
        .andExpect(status().isUnauthorized());

    then(facade).shouldHaveNoInteractions();
  }

  @Test
  @WithMockUser(roles = "USER")
  @DisplayName("일반 사용자는 발표 평가 항목 API에 접근할 수 없다")
  void userForbidden() throws Exception {
    mockMvc.perform(get(URL, 2L))
        .andExpect(status().isForbidden());

    then(facade).shouldHaveNoInteractions();
  }

  @Test
  @WithMockUser(username = "202012345", roles = "ADMIN")
  @DisplayName("평가 항목 목록을 표시 순서대로 응답한다")
  void getCriteria() throws Exception {
    given(facade.getCriteria(2L, "202012345"))
        .willReturn(new TeamEvaluationCriterionListResponse(List.of(
        new TeamEvaluationCriterionResponse(1L, "객체지향 설계", 30, 0))));

    mockMvc.perform(get(URL, 2L))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.contents[0].id").value(1L))
        .andExpect(jsonPath("$.contents[0].title").value("객체지향 설계"))
        .andExpect(jsonPath("$.contents[0].maxScore").value(30))
        .andExpect(jsonPath("$.contents[0].displayOrder").value(0));
  }

  @Test
  @WithMockUser(username = "202012345", roles = "ADMIN")
  @DisplayName("유효한 평가 항목 생성 요청은 201을 응답한다")
  void createCriterion() throws Exception {
    given(facade.createCriterion(
        2L,
        "202012345",
        new TeamEvaluationCriterionCreateRequest("객체지향 설계", 30, 0)))
        .willReturn(TeamEvaluationCriterionPersistResponse.of(1L));

    mockMvc.perform(post(URL, 2L).with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(VALID_BODY))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(1L));
  }

  @Test
  @WithMockUser(username = "202012345", roles = "ADMIN")
  @DisplayName("앞뒤 공백 제거 후 100자인 평가 항목명은 생성 요청을 허용한다")
  void createCriterion_WithPaddedMaximumLengthTitle() throws Exception {
    String title = " " + "가".repeat(100) + "  ";
    TeamEvaluationCriterionCreateRequest request =
        new TeamEvaluationCriterionCreateRequest(title, 30, 0);
    given(facade.createCriterion(2L, "202012345", request))
        .willReturn(TeamEvaluationCriterionPersistResponse.of(1L));

    mockMvc.perform(post(URL, 2L).with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"title\":\"" + title + "\",\"maxScore\":30,\"displayOrder\":0}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(1L));

    then(facade).should().createCriterion(2L, "202012345", request);
  }

  @Test
  @WithMockUser(username = "202012345", roles = "ADMIN")
  @DisplayName("담당 교수가 아닌 관리자는 발표 평가 항목을 조회할 수 없다")
  void anotherProfessorForbidden() throws Exception {
    willThrow(new AccessDeniedException("담당 분반만 접근할 수 있습니다."))
        .given(facade)
        .getCriteria(2L, "202012345");

    mockMvc.perform(get(URL, 2L))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
  }

  @ParameterizedTest(name = "sectionId={0}")
  @ValueSource(longs = {0L, -1L})
  @WithMockUser(roles = "ADMIN")
  @DisplayName("0 이하 분반 id는 400을 응답한다")
  void rejectNonPositiveSectionId(long sectionId) throws Exception {
    mockMvc.perform(get(URL, sectionId))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_INPUT"));

    then(facade).shouldHaveNoInteractions();
  }

  @ParameterizedTest(name = "body={0}")
  @ValueSource(strings = {
      "{\"title\":\"\",\"maxScore\":30,\"displayOrder\":0}",
      "{\"title\":\"객체지향 설계\",\"maxScore\":0,\"displayOrder\":0}",
      "{\"title\":\"객체지향 설계\",\"maxScore\":30,\"displayOrder\":-1}"
  })
  @WithMockUser(roles = "ADMIN")
  @DisplayName("유효하지 않은 평가 항목 생성 요청은 400을 응답한다")
  void rejectInvalidRequest(String body) throws Exception {
    mockMvc.perform(post(URL, 2L).with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_INPUT"));

    then(facade).shouldHaveNoInteractions();
  }
}
