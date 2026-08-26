package evaluation.presentation;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import kgu.developers.admin.evaluation.application.TeamEvaluationCriterionFacade;
import kgu.developers.admin.evaluation.presentation.TeamEvaluationCriterionControllerImpl;
import kgu.developers.admin.evaluation.presentation.response.TeamEvaluationCriterionListResponse;
import kgu.developers.admin.evaluation.presentation.response.TeamEvaluationCriterionPersistResponse;
import kgu.developers.admin.evaluation.presentation.response.TeamEvaluationCriterionResponse;
import kgu.developers.common.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest
@Import({TeamEvaluationCriterionControllerImpl.class, GlobalExceptionHandler.class})
@WithMockUser(roles = "ADMIN")
@TestPropertySource(properties = {
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

  @Test
  @DisplayName("평가 항목 목록을 표시 순서대로 응답한다")
  void getCriteria() throws Exception {
    given(facade.getCriteria(2L)).willReturn(new TeamEvaluationCriterionListResponse(List.of(
        new TeamEvaluationCriterionResponse(1L, "객체지향 설계", 30, 0))));

    mockMvc.perform(get(URL, 2L))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.contents[0].id").value(1L))
        .andExpect(jsonPath("$.contents[0].title").value("객체지향 설계"))
        .andExpect(jsonPath("$.contents[0].maxScore").value(30))
        .andExpect(jsonPath("$.contents[0].displayOrder").value(0));
  }

  @Test
  @DisplayName("유효한 평가 항목 생성 요청은 201을 응답한다")
  void createCriterion() throws Exception {
    given(facade.createCriterion(
        2L,
        new kgu.developers.admin.evaluation.presentation.request.TeamEvaluationCriterionCreateRequest(
            "객체지향 설계", 30, 0)))
        .willReturn(TeamEvaluationCriterionPersistResponse.of(1L));

    mockMvc.perform(post(URL, 2L).with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(VALID_BODY))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(1L));
  }

  @ParameterizedTest(name = "sectionId={0}")
  @ValueSource(longs = {0L, -1L})
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
