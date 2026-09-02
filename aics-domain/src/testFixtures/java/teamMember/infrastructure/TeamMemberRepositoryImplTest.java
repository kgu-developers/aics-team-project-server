package teamMember.infrastructure;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willAnswer;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.persistence.EntityManager;
import kgu.developers.domain.team.infrastructure.TeamJpaEntity;
import kgu.developers.domain.teamMember.domain.TeamMember;
import kgu.developers.domain.teamMember.infrastructure.JpaTeamMemberRepository;
import kgu.developers.domain.teamMember.infrastructure.TeamMemberJpaEntity;
import kgu.developers.domain.teamMember.infrastructure.TeamMemberRepositoryImpl;
import kgu.developers.domain.user.infrastructure.UserJpaEntity;

@ExtendWith(MockitoExtension.class)
class TeamMemberRepositoryImplTest {

  private static final Long TEAM_ID = 10L;

  @Mock
  private JpaTeamMemberRepository jpaTeamMemberRepository;

  @Mock
  private EntityManager entityManager;

  @InjectMocks
  private TeamMemberRepositoryImpl teamMemberRepositoryImpl;

  private TeamMember member(Long id, String userId, boolean isLeader) {
    return TeamMember.builder()
        .id(id)
        .version(0L)
        .teamId(TEAM_ID)
        .userId(userId)
        .isLeader(isLeader)
        .projectRole("역할")
        .build();
  }

  @Test
  @DisplayName("saveAll은 팀장 강등을 먼저 flush해서 팀장 교체가 실패하지 않는다")
  public void saveAll_LeaderHandover_DemotesBeforeValidatingPromotion() {
    TeamMemberJpaEntity oldLeaderEntity = TeamMemberJpaEntity.builder()
        .id(1L)
        .team(TeamJpaEntity.builder().id(TEAM_ID).build())
        .user(UserJpaEntity.builder().studentNumber("202412345").build())
        .isLeader(true)
        .projectRole("역할")
        .build();

    AtomicBoolean flushed = new AtomicBoolean(false);
    willAnswer(invocation -> {
      flushed.set(true);
      return null;
    }).given(jpaTeamMemberRepository).flush();
    given(jpaTeamMemberRepository.findByTeamIdAndIsLeaderTrueAndDeletedAtIsNull(TEAM_ID))
        .willAnswer(invocation -> flushed.get() ? Optional.empty() : Optional.of(oldLeaderEntity));

    given(entityManager.getReference(eq(TeamJpaEntity.class), any()))
        .willReturn(TeamJpaEntity.builder().id(TEAM_ID).build());
    given(entityManager.getReference(eq(UserJpaEntity.class), any()))
        .willAnswer(invocation -> UserJpaEntity.builder().studentNumber(invocation.getArgument(1)).build());
    given(jpaTeamMemberRepository.saveAll(anyList())).willAnswer(invocation -> invocation.getArgument(0));

    List<TeamMember> handover = List.of(
        member(1L, "202412345", false),
        member(2L, "202412346", true));

    assertThatCode(() -> teamMemberRepositoryImpl.saveAll(handover)).doesNotThrowAnyException();
  }
}
