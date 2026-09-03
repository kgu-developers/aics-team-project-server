package evaluation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.util.List;
import kgu.developers.admin.evaluation.application.TeamEvaluationCriterionFacade;
import kgu.developers.admin.evaluation.presentation.request.TeamEvaluationCriterionCreateRequest;
import kgu.developers.domain.evaluation.application.command.TeamEvaluationCriterionCommandService;
import kgu.developers.domain.evaluation.application.query.TeamEvaluationCriterionQueryService;
import kgu.developers.domain.evaluation.domain.TeamEvaluationCriterion;
import kgu.developers.domain.section.application.query.SectionQueryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class TeamEvaluationCriterionFacadeTest {

  @Mock
  private TeamEvaluationCriterionCommandService commandService;

  @Mock
  private TeamEvaluationCriterionQueryService queryService;

  @Mock
  private SectionQueryService sectionQueryService;

  @InjectMocks
  private TeamEvaluationCriterionFacade facade;

  @Test
  @DisplayName("평가 항목 생성 요청을 커맨드 서비스에 전달하고 id를 응답한다")
  void createCriterion() {
    TeamEvaluationCriterionCreateRequest request =
        new TeamEvaluationCriterionCreateRequest("객체지향 설계", 30, 0);
    given(sectionQueryService.isActiveSectionOwnedByProfessor(2L, "202012345"))
        .willReturn(true);
    given(commandService.createCriterion(2L, "객체지향 설계", 30, 0)).willReturn(1L);

    assertThat(facade.createCriterion(2L, "202012345", request).id()).isEqualTo(1L);
  }

  @Test
  @DisplayName("분반별 평가 항목을 응답 DTO로 변환한다")
  void getCriteria() {
    given(sectionQueryService.isActiveSectionOwnedByProfessor(2L, "202012345"))
        .willReturn(true);
    given(queryService.getCriteria(2L)).willReturn(List.of(
        TeamEvaluationCriterion.restore(
            1L, 2L, "객체지향 설계", 30, 0, null, null, null)));

    assertThat(facade.getCriteria(2L, "202012345").contents())
        .singleElement()
        .satisfies(response -> {
          assertThat(response.id()).isEqualTo(1L);
          assertThat(response.title()).isEqualTo("객체지향 설계");
          assertThat(response.maxScore()).isEqualTo(30);
          assertThat(response.displayOrder()).isZero();
        });
  }

  @Test
  @DisplayName("담당 교수가 아닌 관리자는 평가 항목을 조회할 수 없다")
  void rejectAnotherProfessor() {
    given(sectionQueryService.isActiveSectionOwnedByProfessor(2L, "202012345"))
        .willReturn(false);

    assertThatThrownBy(() -> facade.getCriteria(2L, "202012345"))
        .isInstanceOf(AccessDeniedException.class);

    then(queryService).shouldHaveNoInteractions();
  }

  @Test
  @DisplayName("담당 교수가 아닌 관리자는 평가 항목을 생성할 수 없다")
  void rejectCreateByAnotherProfessor() {
    TeamEvaluationCriterionCreateRequest request =
        new TeamEvaluationCriterionCreateRequest("객체지향 설계", 30, 0);
    given(sectionQueryService.isActiveSectionOwnedByProfessor(2L, "202012345"))
        .willReturn(false);

    assertThatThrownBy(() -> facade.createCriterion(2L, "202012345", request))
        .isInstanceOf(AccessDeniedException.class);

    then(commandService).shouldHaveNoInteractions();
  }

  @Test
  @DisplayName("존재하지 않는 분반도 소유하지 않은 분반과 동일하게 거부한다")
  void rejectMissingSection() {
    given(sectionQueryService.isActiveSectionOwnedByProfessor(404L, "202012345"))
        .willReturn(false);

    assertThatThrownBy(() -> facade.getCriteria(404L, "202012345"))
        .isInstanceOf(AccessDeniedException.class);

    then(queryService).shouldHaveNoInteractions();
  }
}
