package evaluation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.util.List;
import kgu.developers.admin.evaluation.application.TeamEvaluationCriterionFacade;
import kgu.developers.admin.evaluation.presentation.request.TeamEvaluationCriterionCreateRequest;
import kgu.developers.domain.evaluation.application.command.TeamEvaluationCriterionCommandService;
import kgu.developers.domain.evaluation.application.query.TeamEvaluationCriterionQueryService;
import kgu.developers.domain.evaluation.domain.TeamEvaluationCriterion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TeamEvaluationCriterionFacadeTest {

  @Mock
  private TeamEvaluationCriterionCommandService commandService;

  @Mock
  private TeamEvaluationCriterionQueryService queryService;

  @InjectMocks
  private TeamEvaluationCriterionFacade facade;

  @Test
  @DisplayName("평가 항목 생성 요청을 커맨드 서비스에 전달하고 id를 응답한다")
  void createCriterion() {
    TeamEvaluationCriterionCreateRequest request =
        new TeamEvaluationCriterionCreateRequest("객체지향 설계", 30, 0);
    given(commandService.createCriterion(2L, "객체지향 설계", 30, 0)).willReturn(1L);

    assertThat(facade.createCriterion(2L, request).id()).isEqualTo(1L);
  }

  @Test
  @DisplayName("분반별 평가 항목을 응답 DTO로 변환한다")
  void getCriteria() {
    given(queryService.getCriteria(2L)).willReturn(List.of(
        TeamEvaluationCriterion.restore(
            1L, 2L, "객체지향 설계", 30, 0, null, null, null)));

    assertThat(facade.getCriteria(2L).contents())
        .singleElement()
        .satisfies(response -> {
          assertThat(response.id()).isEqualTo(1L);
          assertThat(response.title()).isEqualTo("객체지향 설계");
          assertThat(response.maxScore()).isEqualTo(30);
          assertThat(response.displayOrder()).isZero();
        });
  }
}
