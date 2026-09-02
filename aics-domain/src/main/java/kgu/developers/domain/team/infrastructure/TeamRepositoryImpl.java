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
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static jakarta.persistence.LockModeType.PESSIMISTIC_WRITE;
import static kgu.developers.common.exception.ConstraintViolations.violates;
import static kgu.developers.common.exception.OptimisticLocks.translate;

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
            return translate(
                    () -> jpaTeamRepository.saveAndFlush(entity).toDomain(),
                    TeamConcurrentlyModifiedException::new);
        } catch (DataIntegrityViolationException e) {
            if (violates(e, TEAM_NAME_INDEX)) {
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
    public Optional<Team> findByIdForUpdate(Long id) {
        return jpaTeamRepository.findByIdForUpdate(id)
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
    public boolean existsBySectionIdAndNameAndIdNot(Long sectionId, String name, Long id) {
        // 분반 행을 먼저 잠가서, 같은 분반에 팀명이 동시에 중복 등록되는 경쟁 상태를 막는다.
        // DB 유니크 인덱스(uk_team_section_name)는 database/team.sql로만 관리돼 환경마다
        // 실제로 적용됐는지 보장할 수 없어 이 애플리케이션 락이 최종 방어선이다.
        entityManager.find(SectionJpaEntity.class, sectionId, PESSIMISTIC_WRITE);
        return jpaTeamRepository.existsBySectionIdAndNameAndIdNotAndDeletedAtIsNull(sectionId, name, id);
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
