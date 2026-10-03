package evaluation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.util.List;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import kgu.developers.admin.evaluation.application.TeamEvaluationCriterionFacade;
import kgu.developers.admin.evaluation.presentation.request.TeamEvaluationCriterionCreateRequest;
import kgu.developers.admin.evaluation.presentation.request.TeamEvaluationCriterionUpdateRequest;
import kgu.developers.domain.evaluation.application.command.TeamEvaluationCriterionCommandService;
import kgu.developers.domain.evaluation.application.query.TeamEvaluationCriterionQueryService;
import kgu.developers.domain.evaluation.domain.TeamEvaluationCriterion;
import kgu.developers.domain.evaluation.domain.TeamEvaluationScoreRepository;
import kgu.developers.domain.evaluation.domain.ProfessorPresentationEvaluationScoreRepository;
import kgu.developers.domain.evaluation.exception.TeamEvaluationCriterionLockedException;
import kgu.developers.domain.milestone.domain.Milestone;
import kgu.developers.domain.milestone.domain.MilestoneRepository;
import kgu.developers.domain.milestone.domain.MilestoneSchedule;
import kgu.developers.domain.milestone.domain.MilestoneType;
import kgu.developers.domain.section.application.query.SectionQueryService;
import kgu.developers.domain.section.domain.SectionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
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

  @Mock
  private SectionRepository sectionRepository;

  @Mock
  private MilestoneRepository milestoneRepository;

  @Mock
  private TeamEvaluationScoreRepository scoreRepository;

  @Mock
  private ProfessorPresentationEvaluationScoreRepository professorScoreRepository;

  @Spy
  private Clock serviceClock = Clock.fixed(Instant.parse("2026-10-02T01:00:00Z"), ZoneId.of("Asia/Seoul"));

  @InjectMocks
  private TeamEvaluationCriterionFacade facade;

  @Test
  @DisplayName("평가 항목 생성 요청을 커맨드 서비스에 전달하고 id를 응답한다")
  void createCriterion() {
    TeamEvaluationCriterionCreateRequest request =
        new TeamEvaluationCriterionCreateRequest("객체지향 설계", 30, 0);
    given(sectionRepository.lockActiveByIdAndProfessorId(2L, "202012345"))
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
    assertThatThrownBy(() -> facade.getCriteria(2L, "202012345"))
        .isInstanceOf(AccessDeniedException.class);

    then(queryService).shouldHaveNoInteractions();
  }

  @Test
  @DisplayName("담당 교수가 아닌 관리자는 평가 항목을 생성할 수 없다")
  void rejectCreateByAnotherProfessor() {
    TeamEvaluationCriterionCreateRequest request =
        new TeamEvaluationCriterionCreateRequest("객체지향 설계", 30, 0);
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

  @Test
  @DisplayName("평가 전에는 담당 교수자가 항목을 수정할 수 있다")
  void updateCriterionBeforeEvaluation() {
    TeamEvaluationCriterion criterion = TeamEvaluationCriterion.restore(
        1L, 2L, "기존 항목", 10, 0, null, null, null);
    TeamEvaluationCriterionUpdateRequest request =
        new TeamEvaluationCriterionUpdateRequest("새 항목", 20, 1);
    given(sectionRepository.lockActiveByIdAndProfessorId(2L, "202012345")).willReturn(true);
    given(queryService.getCriterion(1L)).willReturn(criterion);

    facade.updateCriterion(2L, 1L, "202012345", request);

    then(commandService).should().updateCriterion(criterion, "새 항목", 20, 1);
  }

  @Test
  @DisplayName("평가가 시작됐다면 항목을 수정할 수 없다")
  void rejectUpdateAfterEvaluationStarted() {
    TeamEvaluationCriterion criterion = TeamEvaluationCriterion.restore(
        1L, 2L, "기존 항목", 10, 0, null, null, null);
    given(sectionRepository.lockActiveByIdAndProfessorId(2L, "202012345")).willReturn(true);
    given(queryService.getCriterion(1L)).willReturn(criterion);
    given(milestoneRepository.findAllBySectionIdOrderByWeekNumber(2L)).willReturn(List.of(
        Milestone.create(2L, "발표", null, 1,
            new MilestoneSchedule(null, LocalDateTime.parse("2026-10-01T09:00:00"), null, null,
                LocalDateTime.parse("2026-10-02T09:00:00"), LocalDateTime.parse("2026-10-03T09:00:00")),
            MilestoneType.PRESENTATION)));

    assertThatThrownBy(() -> facade.updateCriterion(2L, 1L, "202012345",
        new TeamEvaluationCriterionUpdateRequest("새 항목", 20, 1)))
        .isInstanceOf(TeamEvaluationCriterionLockedException.class);
    then(commandService).shouldHaveNoInteractions();
  }

  @Test
  @DisplayName("학생 점수가 저장됐다면 항목을 삭제할 수 없다")
  void rejectDeleteWhenScored() {
    TeamEvaluationCriterion criterion = TeamEvaluationCriterion.restore(
        1L, 2L, "기존 항목", 10, 0, null, null, null);
    given(sectionRepository.lockActiveByIdAndProfessorId(2L, "202012345")).willReturn(true);
    given(queryService.getCriterion(1L)).willReturn(criterion);
    given(scoreRepository.existsByCriterionId(1L)).willReturn(true);

    assertThatThrownBy(() -> facade.deleteCriterion(2L, 1L, "202012345"))
        .isInstanceOf(TeamEvaluationCriterionLockedException.class);
    then(commandService).shouldHaveNoInteractions();
  }

  @Test
  @DisplayName("교수자 점수가 저장됐다면 항목을 삭제할 수 없다")
  void rejectDeleteWhenProfessorScored() {
    TeamEvaluationCriterion criterion = TeamEvaluationCriterion.restore(
        1L, 2L, "기존 항목", 10, 0, null, null, null);
    given(sectionRepository.lockActiveByIdAndProfessorId(2L, "202012345")).willReturn(true);
    given(queryService.getCriterion(1L)).willReturn(criterion);
    given(professorScoreRepository.existsByCriterionId(1L)).willReturn(true);

    assertThatThrownBy(() -> facade.deleteCriterion(2L, 1L, "202012345"))
        .isInstanceOf(TeamEvaluationCriterionLockedException.class);
    then(commandService).shouldHaveNoInteractions();
  }
}
