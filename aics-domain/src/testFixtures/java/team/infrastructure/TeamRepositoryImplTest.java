package team.infrastructure;

import static jakarta.persistence.LockModeType.PESSIMISTIC_WRITE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.persistence.EntityManager;
import kgu.developers.domain.section.infrastructure.SectionJpaEntity;
import kgu.developers.domain.team.domain.Status;
import kgu.developers.domain.team.domain.Team;
import kgu.developers.domain.team.infrastructure.JpaTeamRepository;
import kgu.developers.domain.team.infrastructure.TeamJpaEntity;
import kgu.developers.domain.team.infrastructure.TeamRepositoryImpl;
import kgu.developers.domain.teamMember.domain.TeamMemberRepository;

@ExtendWith(MockitoExtension.class)
class TeamRepositoryImplTest {

  private static final Long SECTION_ID = 10L;

  @Mock
  private JpaTeamRepository jpaTeamRepository;

  @Mock
  private TeamMemberRepository teamMemberRepository;

  @Mock
  private EntityManager entityManager;

  @InjectMocks
  private TeamRepositoryImpl teamRepositoryImpl;

  @Test
  @DisplayName("팀명 중복 확인 전에 분반 행을 잠가서 동시 등록 경쟁 상태를 막는다")
  void existsBySectionIdAndNameAndIdNot_LocksSectionBeforeChecking() {
    given(jpaTeamRepository.existsBySectionIdAndNameAndIdNotAndDeletedAtIsNull(SECTION_ID, "1팀", 1L))
        .willReturn(false);

    boolean exists = teamRepositoryImpl.existsBySectionIdAndNameAndIdNot(SECTION_ID, "1팀", 1L);

    assertThat(exists).isFalse();
    verify(entityManager).find(SectionJpaEntity.class, SECTION_ID, PESSIMISTIC_WRITE);
  }

  @Test
  @DisplayName("findByIdForUpdate는 잠금 쿼리로 팀을 조회한다")
  void findByIdForUpdate_DelegatesToLockingQuery() {
    TeamJpaEntity entity = TeamJpaEntity.builder()
        .id(1L)
        .section(SectionJpaEntity.builder().id(SECTION_ID).build())
        .name("1팀")
        .status(Status.FORMING)
        .build();
    given(jpaTeamRepository.findByIdForUpdate(1L)).willReturn(Optional.of(entity));

    Optional<Team> found = teamRepositoryImpl.findByIdForUpdate(1L);

    assertThat(found).isPresent();
    assertThat(found.get().getId()).isEqualTo(1L);
    verify(jpaTeamRepository).findByIdForUpdate(1L);
  }
}
