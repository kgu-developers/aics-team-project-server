package evaluation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import kgu.developers.admin.evaluation.application.ProfessorPresentationEvaluationAdminFacade;
import kgu.developers.admin.evaluation.presentation.request.ProfessorPresentationEvaluationAdminRequest;
import kgu.developers.admin.evaluation.presentation.request.ProfessorPresentationScoreAdminRequest;
import kgu.developers.domain.evaluation.domain.ProfessorPresentationEvaluation;
import kgu.developers.domain.evaluation.domain.ProfessorPresentationEvaluationRepository;
import kgu.developers.domain.evaluation.domain.ProfessorPresentationEvaluationScore;
import kgu.developers.domain.evaluation.domain.ProfessorPresentationEvaluationScoreRepository;
import kgu.developers.domain.evaluation.domain.TeamEvaluationCriterion;
import kgu.developers.domain.evaluation.domain.TeamEvaluationCriterionRepository;
import kgu.developers.domain.evaluation.exception.InvalidProfessorPresentationEvaluationException;
import kgu.developers.domain.evaluation.exception.TeamEvaluationClosedException;
import kgu.developers.domain.milestone.domain.Milestone;
import kgu.developers.domain.milestone.domain.MilestoneRepository;
import kgu.developers.domain.milestone.domain.MilestoneSchedule;
import kgu.developers.domain.milestone.domain.MilestoneStatus;
import kgu.developers.domain.milestone.domain.MilestoneType;
import kgu.developers.domain.section.domain.SectionRepository;
import kgu.developers.domain.team.domain.Team;
import kgu.developers.domain.team.domain.TeamRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class ProfessorPresentationEvaluationAdminFacadeTest {
    @Mock private SectionRepository sectionRepository;
    @Mock private MilestoneRepository milestoneRepository;
    @Mock private TeamRepository teamRepository;
    @Mock private TeamEvaluationCriterionRepository criterionRepository;
    @Mock private ProfessorPresentationEvaluationRepository evaluationRepository;
    @Mock private ProfessorPresentationEvaluationScoreRepository scoreRepository;
    @Spy private Clock serviceClock = Clock.fixed(Instant.parse("2026-10-02T01:00:00Z"), ZoneId.of("Asia/Seoul"));
    @InjectMocks private ProfessorPresentationEvaluationAdminFacade facade;

    @Test
    void saveAllCriteriaAndPrivateMemo() {
        allowSave(false);
        given(criterionRepository.findAllBySectionIdOrderByDisplayOrder(2L)).willReturn(criteria());
        ProfessorPresentationEvaluation saved = ProfessorPresentationEvaluation.restore(
                9L, 3L, 20L, "professor", "비공개 메모", LocalDateTime.now(serviceClock), null, null);
        given(evaluationRepository.findByMilestoneIdAndTeamId(3L, 20L))
                .willReturn(Optional.empty(), Optional.of(saved));
        given(evaluationRepository.save(any())).willReturn(saved);
        given(scoreRepository.findAllByEvaluationId(9L)).willReturn(List.of(
                ProfessorPresentationEvaluationScore.create(9L, 1L, 8, 10)));

        var response = facade.saveEvaluation(2L, 3L, 20L, "professor", request(1L, 8));

        assertThat(response.memo()).isEqualTo("비공개 메모");
        assertThat(response.editable()).isTrue();
        assertThat(response.scores()).singleElement().satisfies(score -> assertThat(score.score()).isEqualTo(8));
        then(scoreRepository).should().replaceAll(9L, List.of(
                ProfessorPresentationEvaluationScore.create(9L, 1L, 8, 10)));
    }

    @Test
    void closedWindowRejectsSave() {
        allowSave(true);
        assertThatThrownBy(() -> facade.saveEvaluation(2L, 3L, 20L, "professor", request(1L, 8)))
                .isInstanceOf(TeamEvaluationClosedException.class);
        then(evaluationRepository).shouldHaveNoInteractions();
    }

    @Test
    void closedWindowStillAllowsPrivateRead() {
        Milestone closedMilestone = milestone(true);
        ProfessorPresentationEvaluation saved = ProfessorPresentationEvaluation.restore(9L, 3L, 20L, "professor", "보존된 메모",
                LocalDateTime.now(serviceClock).minusHours(2), null, null);
        given(sectionRepository.existsActiveByIdAndProfessorId(2L, "professor")).willReturn(true);
        given(milestoneRepository.findByIdAndSectionId(3L, 2L)).willReturn(Optional.of(closedMilestone));
        given(teamRepository.findById(20L)).willReturn(Optional.of(Team.builder().id(20L).sectionId(2L).build()));
        given(criterionRepository.findAllBySectionIdOrderByDisplayOrder(2L)).willReturn(criteria());
        given(evaluationRepository.findByMilestoneIdAndTeamId(3L, 20L)).willReturn(Optional.of(saved));
        assertThat(facade.getEvaluation(2L, 3L, 20L, "professor"))
                .satisfies(response -> {
                    assertThat(response.editable()).isFalse();
                    assertThat(response.memo()).isEqualTo("보존된 메모");
                });
    }

    @Test
    void otherProfessorCannotReadMemo() {
        assertThatThrownBy(() -> facade.getEvaluation(2L, 3L, 20L, "other"))
                .isInstanceOf(AccessDeniedException.class);
        then(evaluationRepository).shouldHaveNoInteractions();
    }

    @Test
    void missingCriterionAndOutOfRangeScoreAreRejected() {
        allowSave(false);
        given(criterionRepository.findAllBySectionIdOrderByDisplayOrder(2L)).willReturn(criteria());
        assertThatThrownBy(() -> facade.saveEvaluation(2L, 3L, 20L, "professor", request(2L, 8)))
                .isInstanceOf(InvalidProfessorPresentationEvaluationException.class);
        assertThatThrownBy(() -> facade.saveEvaluation(2L, 3L, 20L, "professor", request(1L, 11)))
                .isInstanceOf(InvalidProfessorPresentationEvaluationException.class);
        then(evaluationRepository).shouldHaveNoInteractions();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void missingAndOtherSectionTeamsAreForbiddenForRead(boolean otherSection) {
        Milestone evaluationMilestone = milestone(false);
        given(sectionRepository.existsActiveByIdAndProfessorId(2L, "professor")).willReturn(true);
        given(milestoneRepository.findByIdAndSectionId(3L, 2L)).willReturn(Optional.of(evaluationMilestone));
        given(teamRepository.findById(20L)).willReturn(otherSection
                ? Optional.of(Team.builder().id(20L).sectionId(99L).build()) : Optional.empty());

        assertThatThrownBy(() -> facade.getEvaluation(2L, 3L, 20L, "professor"))
                .isInstanceOf(AccessDeniedException.class);
        then(evaluationRepository).shouldHaveNoInteractions();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void missingAndOtherSectionTeamsAreForbiddenForSave(boolean otherSection) {
        allowSave(false);
        given(teamRepository.findById(20L)).willReturn(otherSection
                ? Optional.of(Team.builder().id(20L).sectionId(99L).build()) : Optional.empty());

        assertThatThrownBy(() -> facade.saveEvaluation(2L, 3L, 20L, "professor", request(1L, 8)))
                .isInstanceOf(AccessDeniedException.class);
        then(evaluationRepository).shouldHaveNoInteractions();
    }

    @Test
    void saveUsesOneTimeSnapshotEvenWhenProcessingCrossesClosingTime() {
        allowSave(false);
        given(criterionRepository.findAllBySectionIdOrderByDisplayOrder(2L)).willReturn(criteria());
        LocalDateTime acceptedAt = LocalDateTime.of(2026, 10, 2, 10, 59, 59);
        ProfessorPresentationEvaluation saved = ProfessorPresentationEvaluation.restore(
                9L, 3L, 20L, "professor", "비공개 메모", acceptedAt, null, null);
        given(evaluationRepository.findByMilestoneIdAndTeamId(3L, 20L))
                .willReturn(Optional.empty(), Optional.of(saved));
        given(evaluationRepository.save(any())).willAnswer(invocation -> {
            assertThat(((ProfessorPresentationEvaluation) invocation.getArgument(0)).getSubmittedAt())
                    .isEqualTo(acceptedAt);
            return saved;
        });
        doReturn(Instant.parse("2026-10-02T01:59:59Z"), Instant.parse("2026-10-02T02:00:01Z"))
                .when(serviceClock).instant();
        clearInvocations(serviceClock);

        var response = facade.saveEvaluation(2L, 3L, 20L, "professor", request(1L, 8));

        assertThat(response.editable()).isTrue();
        assertThat(response.submittedAt()).isEqualTo(acceptedAt);
        then(serviceClock).should(times(1)).instant();
    }

    private void allowSave(boolean closed) {
        Milestone evaluationMilestone = milestone(closed);
        given(sectionRepository.lockActiveByIdAndProfessorId(2L, "professor")).willReturn(true);
        given(milestoneRepository.findByIdAndSectionIdForUpdate(3L, 2L)).willReturn(Optional.of(evaluationMilestone));
        given(teamRepository.findById(20L)).willReturn(Optional.of(Team.builder().id(20L).sectionId(2L).build()));
    }

    private Milestone milestone(boolean closed) {
        LocalDateTime now = LocalDateTime.now(serviceClock);
        return Milestone.restore(3L, 2L, "발표", null, 1, MilestoneStatus.PUBLISHED,
                new MilestoneSchedule(null, now.plusDays(2), null, null, now.minusHours(3),
                        closed ? now.minusHours(1) : now.plusHours(1), true), MilestoneType.PRESENTATION);
    }

    private List<TeamEvaluationCriterion> criteria() {
        return List.of(TeamEvaluationCriterion.restore(1L, 2L, "완성도", 10, 0, null, null, null));
    }

    private ProfessorPresentationEvaluationAdminRequest request(Long criterionId, int score) {
        return new ProfessorPresentationEvaluationAdminRequest(
                List.of(new ProfessorPresentationScoreAdminRequest(criterionId, score)), "비공개 메모");
    }
}
