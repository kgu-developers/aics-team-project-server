package evaluation.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.List;
import kgu.developers.domain.evaluation.infrastructure.JpaTeamEvaluationCriterionRepository;
import kgu.developers.domain.evaluation.infrastructure.TeamEvaluationCriterionJpaEntity;
import kgu.developers.domain.evaluation.infrastructure.TeamEvaluationCriterionRepositoryImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TeamEvaluationCriterionRepositoryImplTest {

  @Mock
  private JpaTeamEvaluationCriterionRepository jpaRepository;

  @InjectMocks
  private TeamEvaluationCriterionRepositoryImpl repository;

  @Test
  @DisplayName("분반 평가 항목은 표시 순서 오름차순 조회 쿼리를 사용한다")
  void findAllBySectionIdOrderByDisplayOrder() {
    given(jpaRepository.findAllBySectionIdAndDeletedAtIsNullOrderByDisplayOrderAsc(2L))
        .willReturn(List.of(
            criterion(1L, 0, "설계"),
            criterion(2L, 1, "구현")));

    assertThat(repository.findAllBySectionIdOrderByDisplayOrder(2L))
        .extracting("displayOrder")
        .containsExactly(0, 1);
    verify(jpaRepository).findAllBySectionIdAndDeletedAtIsNullOrderByDisplayOrderAsc(2L);
  }

  private TeamEvaluationCriterionJpaEntity criterion(Long id, int displayOrder, String title) {
    return TeamEvaluationCriterionJpaEntity.builder()
        .id(id)
        .sectionId(2L)
        .title(title)
        .maxScore(10)
        .displayOrder(displayOrder)
        .build();
  }
}
