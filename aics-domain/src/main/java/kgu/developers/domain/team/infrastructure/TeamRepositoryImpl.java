package kgu.developers.domain.team.infrastructure;

import jakarta.persistence.EntityManager;
import kgu.developers.domain.section.infrastructure.SectionJpaEntity;
import kgu.developers.domain.team.domain.Team;
import kgu.developers.domain.team.domain.TeamRepository;
import kgu.developers.domain.team.exception.DuplicateTeamNameException;
import kgu.developers.domain.team.exception.TeamConcurrentlyModifiedException;
import kgu.developers.domain.team.exception.TeamNotFoundException;
import kgu.developers.domain.teamMember.domain.TeamMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class TeamRepositoryImpl implements TeamRepository {
    private static final String TEAM_NAME_INDEX = "uk_team_section_name";

    private final JpaTeamRepository jpaTeamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final EntityManager entityManager;

    @Override
    public Team save(Team team) {
        SectionJpaEntity section = entityManager.getReference(SectionJpaEntity.class, team.getSectionId());
        TeamJpaEntity entity = TeamJpaEntity.toEntity(team, section);
        try {
            return jpaTeamRepository.saveAndFlush(entity).toDomain();
        } catch (OptimisticLockingFailureException e) {
            throw new TeamConcurrentlyModifiedException();
        } catch (DataIntegrityViolationException e) {
            if (Optional.ofNullable(e.getMostSpecificCause().getMessage()).map(m -> m.contains(TEAM_NAME_INDEX)).orElse(false)) {
                throw new DuplicateTeamNameException();
            }
            throw e;
        }
    }

    @Override
    public Optional<Team> findById(Long id) {
        return jpaTeamRepository.findByIdAndDeletedAtIsNull(id)
                .map(TeamJpaEntity::toDomain);
    }

    @Override
    public List<Team> findAllById(List<Long> ids) {
        return jpaTeamRepository.findAllByIdInAndDeletedAtIsNull(ids)
                .stream()
                .map(TeamJpaEntity::toDomain)
                .toList();
    }

    @Override
    public List<Team> findAllBySectionId(Long sectionId) {
        return jpaTeamRepository.findAllBySectionIdAndDeletedAtIsNull(sectionId)
                .stream()
                .map(TeamJpaEntity::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        TeamJpaEntity team = jpaTeamRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(TeamNotFoundException::new);
        team.delete();
        teamMemberRepository.deleteAllByTeamId(id);
    }
}
