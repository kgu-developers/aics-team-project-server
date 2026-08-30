package kgu.developers.domain.teamMember.infrastructure;

import jakarta.persistence.EntityManager;
import kgu.developers.domain.team.infrastructure.TeamJpaEntity;
import kgu.developers.domain.teamMember.domain.TeamMember;
import kgu.developers.domain.teamMember.domain.TeamMemberRepository;
import kgu.developers.domain.teamMember.exception.LeaderAlreadyExistsException;
import kgu.developers.domain.teamMember.exception.TeamMemberAlreadyExistsException;
import kgu.developers.domain.teamMember.exception.TeamMemberConcurrentlyModifiedException;
import kgu.developers.domain.teamMember.exception.TeamMemberNotFoundException;
import kgu.developers.domain.user.infrastructure.UserJpaEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static jakarta.persistence.LockModeType.PESSIMISTIC_WRITE;
import static java.util.stream.Collectors.partitioningBy;
import static kgu.developers.common.exception.ConstraintViolations.violates;

@Repository
@RequiredArgsConstructor
public class TeamMemberRepositoryImpl implements TeamMemberRepository {
  private static final String TEAM_MEMBER_INDEX = "uk_team_member_team_user";
  private static final String TEAM_LEADER_INDEX = "uk_team_member_one_leader";

  private final JpaTeamMemberRepository jpaTeamMemberRepository;
  private final EntityManager entityManager;

  @Override
  @Transactional
  public TeamMember save(TeamMember teamMember) {
    return saveBatch(List.of(teamMember)).get(0);
  }

  @Override
  @Transactional
  public List<TeamMember> saveAll(List<TeamMember> teamMembers) {
    Map<Boolean, List<TeamMember>> byLeader = teamMembers.stream()
        .collect(partitioningBy(teamMember -> teamMember.isLeader() && teamMember.getDeletedAt() == null));

    List<TeamMember> saved = new ArrayList<>(saveBatch(byLeader.get(false)));
    saved.addAll(saveBatch(byLeader.get(true)));
    return saved;
  }

  private List<TeamMember> saveBatch(List<TeamMember> teamMembers) {
    if (teamMembers.isEmpty()) {
      return List.of();
    }

    List<TeamMemberJpaEntity> entities = teamMembers.stream()
        .map(teamMember -> {
          if (teamMember.isLeader() && teamMember.getDeletedAt() == null) {
            validateNoOtherLeader(teamMember);
          }
          TeamJpaEntity team = entityManager.getReference(TeamJpaEntity.class, teamMember.getTeamId());
          UserJpaEntity user = entityManager.getReference(UserJpaEntity.class, teamMember.getUserId());
          return TeamMemberJpaEntity.toEntity(teamMember, team, user);
        })
        .toList();

    try {
      List<TeamMemberJpaEntity> savedEntities = jpaTeamMemberRepository.saveAll(entities);
      jpaTeamMemberRepository.flush();
      return savedEntities.stream()
          .map(TeamMemberJpaEntity::toDomain)
          .toList();
    } catch (OptimisticLockingFailureException e) {
      throw new TeamMemberConcurrentlyModifiedException();
    } catch (DataIntegrityViolationException e) {
      if (violates(e, TEAM_LEADER_INDEX)) {
        throw new LeaderAlreadyExistsException();
      }
      if (violates(e, TEAM_MEMBER_INDEX)) {
        throw new TeamMemberAlreadyExistsException();
      }
      throw e;
    }
  }

  @Override
  public Optional<TeamMember> findById(Long id) {
    return jpaTeamMemberRepository.findByIdAndDeletedAtIsNull(id)
        .map(TeamMemberJpaEntity::toDomain);
  }

  @Override
  public List<TeamMember> findAllByTeamId(Long teamId) {
    return jpaTeamMemberRepository.findAllByTeamIdAndDeletedAtIsNull(teamId)
        .stream()
        .map(TeamMemberJpaEntity::toDomain)
        .toList();
  }

  @Override
  public List<TeamMember> findAllByUserId(String userId) {
    return jpaTeamMemberRepository.findAllByUserStudentNumberAndDeletedAtIsNull(userId)
        .stream()
        .map(TeamMemberJpaEntity::toDomain)
        .toList();
  }

  @Override
  public Optional<TeamMember> findByTeamIdAndUserId(Long teamId, String userId) {
    return jpaTeamMemberRepository.findByTeamIdAndUserStudentNumberAndDeletedAtIsNull(teamId, userId)
        .map(TeamMemberJpaEntity::toDomain);
  }

  @Override
  public Optional<TeamMember> findLeaderByTeamId(Long teamId) {
    return jpaTeamMemberRepository.findByTeamIdAndIsLeaderTrueAndDeletedAtIsNull(teamId)
        .map(TeamMemberJpaEntity::toDomain);
  }

  @Override
  public boolean existsByTeamIdAndIsLeaderTrue(Long teamId) {
    return jpaTeamMemberRepository.existsByTeamIdAndIsLeaderTrueAndDeletedAtIsNull(teamId);
  }

  private void validateNoOtherLeader(TeamMember teamMember) {
    entityManager.find(TeamJpaEntity.class, teamMember.getTeamId(), PESSIMISTIC_WRITE);
    jpaTeamMemberRepository.findByTeamIdAndIsLeaderTrueAndDeletedAtIsNull(teamMember.getTeamId())
        .filter(leader -> !leader.getId().equals(teamMember.getId()))
        .ifPresent(leader -> {
          throw new LeaderAlreadyExistsException();
        });
  }

  @Override
  @Transactional
  public void deleteById(Long id) {
    jpaTeamMemberRepository.findByIdAndDeletedAtIsNull(id)
        .orElseThrow(TeamMemberNotFoundException::new)
        .delete();
  }

  @Override
  @Transactional
  public void deleteAllByTeamId(Long teamId) {
    jpaTeamMemberRepository.softDeleteAllByTeamId(teamId, LocalDateTime.now());
  }
}
