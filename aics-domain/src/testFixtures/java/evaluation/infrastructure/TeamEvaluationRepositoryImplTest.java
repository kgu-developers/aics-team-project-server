package evaluation.infrastructure;

import kgu.developers.domain.evaluation.domain.TeamEvaluation;
import kgu.developers.domain.evaluation.domain.TeamEvaluationCriterion;
import kgu.developers.domain.evaluation.domain.TeamEvaluationScore;
import kgu.developers.domain.evaluation.infrastructure.JpaTeamEvaluationCriterionRepository;
import kgu.developers.domain.evaluation.infrastructure.JpaTeamEvaluationRepository;
import kgu.developers.domain.evaluation.infrastructure.JpaTeamEvaluationScoreRepository;
import kgu.developers.domain.evaluation.infrastructure.TeamEvaluationCriterionJpaEntity;
import kgu.developers.domain.evaluation.infrastructure.TeamEvaluationCriterionRepositoryImpl;
import kgu.developers.domain.evaluation.infrastructure.TeamEvaluationJpaEntity;
import kgu.developers.domain.evaluation.infrastructure.TeamEvaluationRepositoryImpl;
import kgu.developers.domain.evaluation.infrastructure.TeamEvaluationScoreJpaEntity;
import kgu.developers.domain.evaluation.infrastructure.TeamEvaluationScoreRepositoryImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TeamEvaluationRepositoryImplTest {
    @Mock
    private JpaTeamEvaluationCriterionRepository jpaCriterionRepository;

    @Mock
    private JpaTeamEvaluationRepository jpaEvaluationRepository;

    @Mock
    private JpaTeamEvaluationScoreRepository jpaScoreRepository;

    @Test
    @DisplayName("평가 항목 저장소 어댑터는 저장 결과를 도메인으로 반환한다")
    void saveCriterion() {
        TeamEvaluationCriterionRepositoryImpl repository = new TeamEvaluationCriterionRepositoryImpl(jpaCriterionRepository);
        TeamEvaluationCriterion criterion = TeamEvaluationCriterion.create(1L, "발표 완성도", 10, 1);
        given(jpaCriterionRepository.save(org.mockito.ArgumentMatchers.any(TeamEvaluationCriterionJpaEntity.class)))
                .willReturn(TeamEvaluationCriterionJpaEntity.toEntity(
                        TeamEvaluationCriterion.restore(1L, 1L, "발표 완성도", 10, 1, null, null, null)
                ));

        TeamEvaluationCriterion saved = repository.save(criterion);

        assertThat(saved.getId()).isEqualTo(1L);
        ArgumentCaptor<TeamEvaluationCriterionJpaEntity> captor = ArgumentCaptor.forClass(TeamEvaluationCriterionJpaEntity.class);
        verify(jpaCriterionRepository).save(captor.capture());
        assertThat(captor.getValue().getTitle()).isEqualTo("발표 완성도");
    }

    @Test
    @DisplayName("평가 항목 저장소 어댑터는 분반별 항목을 표시 순서로 조회한다")
    void findCriteriaBySection() {
        TeamEvaluationCriterionRepositoryImpl repository = new TeamEvaluationCriterionRepositoryImpl(jpaCriterionRepository);
        given(jpaCriterionRepository.findAllBySectionIdAndDeletedAtIsNullOrderByDisplayOrderAsc(1L))
                .willReturn(List.of(TeamEvaluationCriterionJpaEntity.toEntity(
                        TeamEvaluationCriterion.restore(2L, 1L, "질의응답", 5, 2, null, null, null)
                )));

        List<TeamEvaluationCriterion> criteria = repository.findAllBySectionIdOrderByDisplayOrder(1L);

        assertThat(criteria).extracting(TeamEvaluationCriterion::getTitle).containsExactly("질의응답");
    }

    @Test
    @DisplayName("팀간 발표평가 저장소 어댑터는 고유키 조건으로 초안을 조회한다")
    void findEvaluationByUniqueKey() {
        TeamEvaluationRepositoryImpl repository = new TeamEvaluationRepositoryImpl(jpaEvaluationRepository);
        given(jpaEvaluationRepository.findByMilestoneIdAndRaterIdAndRateeTeamIdAndDeletedAtIsNull(2L, "20260001", 3L))
                .willReturn(Optional.of(TeamEvaluationJpaEntity.toEntity(
                        TeamEvaluation.restore(1L, 2L, "20260001", 3L, null, null, null, null)
                )));

        Optional<TeamEvaluation> found = repository.findByMilestoneIdAndRaterIdAndRateeTeamId(
                2L, " 20260001 ", 3L);

        assertThat(found).isPresent();
        assertThat(found.get().isSubmitted()).isFalse();
    }

    @Test
    @DisplayName("팀간 발표평가 저장소 어댑터는 평가자별 평가 목록을 조회한다")
    void findEvaluationsByRater() {
        TeamEvaluationRepositoryImpl repository = new TeamEvaluationRepositoryImpl(jpaEvaluationRepository);
        given(jpaEvaluationRepository.findAllByMilestoneIdAndRaterIdAndDeletedAtIsNull(2L, "20260001"))
                .willReturn(List.of(TeamEvaluationJpaEntity.toEntity(
                        TeamEvaluation.restore(1L, 2L, "20260001", 3L, null, null, null, null)
                )));

        List<TeamEvaluation> evaluations = repository.findAllByMilestoneIdAndRaterId(
                2L, " 20260001 ");

        assertThat(evaluations).extracting(TeamEvaluation::getRateeTeamId).containsExactly(3L);
    }

    @Test
    @DisplayName("평가 점수 저장소 어댑터는 평가별 점수 목록을 조회한다")
    void findScoresByEvaluation() {
        TeamEvaluationScoreRepositoryImpl repository = new TeamEvaluationScoreRepositoryImpl(jpaScoreRepository);
        given(jpaScoreRepository.findAllByTeamEvaluationIdAndDeletedAtIsNull(1L))
                .willReturn(List.of(TeamEvaluationScoreJpaEntity.toEntity(
                        TeamEvaluationScore.restore(1L, 1L, 2L, 7, null, null, null)
                )));

        List<TeamEvaluationScore> scores = repository.findAllByTeamEvaluationId(1L);

        assertThat(scores).extracting(TeamEvaluationScore::getScore).containsExactly(7);
    }

    @Test
    @DisplayName("평가 점수 저장소 어댑터는 저장할 때 스칼라 식별자를 JPA entity로 전달한다")
    void saveScore() {
        TeamEvaluationScoreRepositoryImpl repository = new TeamEvaluationScoreRepositoryImpl(jpaScoreRepository);
        TeamEvaluationScore score = TeamEvaluationScore.create(1L, 2L, 7, 10);
        given(jpaScoreRepository.save(org.mockito.ArgumentMatchers.any(TeamEvaluationScoreJpaEntity.class)))
                .willReturn(TeamEvaluationScoreJpaEntity.toEntity(
                        TeamEvaluationScore.restore(3L, 1L, 2L, 7, null, null, null)
                ));

        TeamEvaluationScore saved = repository.save(score);

        assertThat(saved.getId()).isEqualTo(3L);
        ArgumentCaptor<TeamEvaluationScoreJpaEntity> captor = ArgumentCaptor.forClass(TeamEvaluationScoreJpaEntity.class);
        verify(jpaScoreRepository).save(captor.capture());
        assertThat(captor.getValue().getTeamEvaluationId()).isEqualTo(1L);
        assertThat(captor.getValue().getCriterionId()).isEqualTo(2L);
    }
}
