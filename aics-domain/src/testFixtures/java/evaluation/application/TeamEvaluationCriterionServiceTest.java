package evaluation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.List;
import kgu.developers.domain.evaluation.application.command.TeamEvaluationCriterionCommandService;
import kgu.developers.domain.evaluation.application.query.TeamEvaluationCriterionQueryService;
import kgu.developers.domain.evaluation.domain.TeamEvaluationCriterion;
import kgu.developers.domain.evaluation.domain.TeamEvaluationCriterionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TeamEvaluationCriterionServiceTest {

  @Mock
  private TeamEvaluationCriterionRepository criterionRepository;

  @InjectMocks
  private TeamEvaluationCriterionCommandService commandService;

  @InjectMocks
  private TeamEvaluationCriterionQueryService queryService;

  @Test
  @DisplayName("평가 항목을 생성하면 저장된 id를 반환한다")
  void createCriterion() {
    given(criterionRepository.save(any(TeamEvaluationCriterion.class)))
        .willReturn(TeamEvaluationCriterion.restore(
            1L, 2L, "객체지향 설계", 30, 0, null, null, null));

    Long id = commandService.createCriterion(2L, "객체지향 설계", 30, 0);

    assertThat(id).isEqualTo(1L);
    ArgumentCaptor<TeamEvaluationCriterion> captor =
        ArgumentCaptor.forClass(TeamEvaluationCriterion.class);
    verify(criterionRepository).save(captor.capture());
    assertThat(captor.getValue().getSectionId()).isEqualTo(2L);
    assertThat(captor.getValue().getTitle()).isEqualTo("객체지향 설계");
    assertThat(captor.getValue().getMaxScore()).isEqualTo(30);
    assertThat(captor.getValue().getDisplayOrder()).isZero();
  }

  @Test
  @DisplayName("분반별 평가 항목을 저장소 순서대로 조회한다")
  void getCriteria() {
    List<TeamEvaluationCriterion> criteria = List.of(
        TeamEvaluationCriterion.restore(
            1L, 2L, "문제 정의와 완성도", 30, 0, null, null, null),
        TeamEvaluationCriterion.restore(
            2L, 2L, "발표 전달력", 40, 1, null, null, null));
    given(criterionRepository.findAllBySectionIdOrderByDisplayOrder(2L))
        .willReturn(criteria);

    assertThat(queryService.getCriteria(2L)).containsExactlyElementsOf(criteria);
  }
}
