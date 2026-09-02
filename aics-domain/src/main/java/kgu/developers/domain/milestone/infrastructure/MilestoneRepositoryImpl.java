package kgu.developers.domain.milestone.infrastructure;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.EntityManager;
import kgu.developers.domain.milestone.domain.Milestone;
import kgu.developers.domain.milestone.domain.MilestoneRepository;
import kgu.developers.domain.milestone.domain.MilestoneStatus;
import kgu.developers.domain.milestone.exception.MilestoneNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MilestoneRepositoryImpl implements MilestoneRepository {

    private final JpaMilestoneRepository jpaMilestoneRepository;
    private final EntityManager entityManager;

    @Override
    @Transactional
    public Milestone save(Milestone milestone) {
        MilestoneJpaEntity entity = entityForSave(milestone);
        return jpaMilestoneRepository.saveAndFlush(entity).toDomain();
    }

    @Override
    @Transactional
    public List<Milestone> saveAllWeekNumberChanges(Long sectionId, List<Milestone> milestones) {
        if (milestones.isEmpty()) {
            return milestones;
        }
        for (Milestone milestone : milestones) {
            if (!sectionId.equals(milestone.getSectionId())) {
                throw new MilestoneNotFoundException(milestone.getId());
            }
            MilestoneJpaEntity entity = entityManager.find(MilestoneJpaEntity.class, milestone.getId());
            if (entity == null || entity.getDeletedAt() != null) {
                throw new MilestoneNotFoundException(milestone.getId());
            }
            entity.updateFromDomain(milestone);
        }
        entityManager.flush();
        return milestones;
    }

    private MilestoneJpaEntity entityForSave(Milestone milestone) {
        if (milestone.getId() == null) {
            return MilestoneJpaEntity.fromDomain(milestone);
        }

        MilestoneJpaEntity entity = entityManager.find(MilestoneJpaEntity.class, milestone.getId());
        if (entity != null && entity.getDeletedAt() != null) {
            entity = null;
        }
        return updateExistingEntity(milestone, entity);
    }

    private MilestoneJpaEntity updateExistingEntity(
            Milestone milestone,
            MilestoneJpaEntity entity
    ) {
        if (entity == null) {
            throw new MilestoneNotFoundException(milestone.getId());
        }
        entity.updateFromDomain(milestone);
        return entity;
    }

    @Override
    public Optional<Milestone> findById(Long id) {
        return jpaMilestoneRepository.findByIdAndDeletedAtIsNull(id)
                .map(MilestoneJpaEntity::toDomain);
    }

    @Override
    public Optional<Milestone> findByIdAndSectionId(Long id, Long sectionId) {
        return jpaMilestoneRepository.findByIdAndSectionIdAndDeletedAtIsNull(id, sectionId)
                .map(MilestoneJpaEntity::toDomain);
    }

    @Override
    @Transactional
    public Optional<Milestone> findByIdAndSectionIdForUpdate(Long id, Long sectionId) {
        return jpaMilestoneRepository.findActiveByIdAndSectionIdForUpdate(id, sectionId)
                .map(MilestoneJpaEntity::toDomain);
    }

    @Override
    public boolean existsBySectionIdAndWeekNumber(Long sectionId, int weekNumber) {
        return jpaMilestoneRepository
                .existsBySectionIdAndWeekNumberAndDeletedAtIsNull(sectionId, weekNumber);
    }

    @Override
    public List<Milestone> findAllBySectionIdOrderByWeekNumber(Long sectionId) {
        return jpaMilestoneRepository
                .findAllBySectionIdAndDeletedAtIsNullOrderByWeekNumberAsc(sectionId)
                .stream()
                .map(MilestoneJpaEntity::toDomain)
                .toList();
    }

    @Override
    public List<Milestone> findAllBySectionIdAndStatusOrderByWeekNumber(
            Long sectionId,
            MilestoneStatus status
    ) {
        return jpaMilestoneRepository
                .findAllBySectionIdAndStatusAndDeletedAtIsNullOrderByWeekNumberAsc(sectionId, status)
                .stream()
                .map(MilestoneJpaEntity::toDomain)
                .toList();
    }
}
