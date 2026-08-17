package kgu.developers.domain.team.infrastructure;

import jakarta.persistence.EntityManager;
import kgu.developers.domain.section.infrastructure.SectionJpaEntity;
import kgu.developers.domain.team.domain.Team;
import kgu.developers.domain.team.domain.TeamRepository;
import kgu.developers.domain.teamMember.domain.TeamMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class TeamRepositoryImpl implements TeamRepository {
    private final JpaTeamRepository jpaTeamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final EntityManager entityManager;

    @Override
    public Team save(Team team) {
        SectionJpaEntity section = entityManager.getReference(SectionJpaEntity.class, team.getSectionId());
        TeamJpaEntity entity = TeamJpaEntity.toEntity(team, section);
        TeamJpaEntity savedEntity = jpaTeamRepository.save(entity);
        return savedEntity.toDomain();
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
        jpaTeamRepository.findByIdAndDeletedAtIsNull(id)
                .ifPresent(team -> {
                    team.delete();
                    teamMemberRepository.deleteAllByTeamId(id);
                });
    }
}
