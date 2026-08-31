package kgu.developers.domain.milestone.infrastructure;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.persistence.EntityManager;
import kgu.developers.domain.milestone.domain.Milestone;
import kgu.developers.domain.milestone.domain.MilestoneRepository;
import kgu.developers.domain.milestone.domain.MilestoneStatus;
import kgu.developers.domain.milestone.exception.MilestoneConcurrentlyModifiedException;
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
        validateFullSectionSnapshot(sectionId, milestones);
        Map<Long, MilestoneJpaEntity> existingEntities = findExistingEntities(milestones);
        validateExistingEntities(milestones, existingEntities);
        List<MilestoneJpaEntity> changedEntities = milestones.stream()
                .filter(milestone -> hasWeekNumberChanged(milestone, existingEntities))
                .map(milestone -> entityForSave(milestone, existingEntities))
                .toList();
        if (!changedEntities.isEmpty()) {
            jpaMilestoneRepository.saveAllAndFlush(changedEntities);
        }
        return milestones;
    }

    private void validateFullSectionSnapshot(Long sectionId, List<Milestone> milestones) {
        boolean containsAnotherSection = milestones.stream()
                .anyMatch(milestone -> !sectionId.equals(milestone.getSectionId()));
        long distinctMilestoneIdCount = milestones.stream()
                .map(Milestone::getId)
                .filter(id -> id != null)
                .distinct()
                .count();
        long activeMilestoneCount = jpaMilestoneRepository.countBySectionIdAndDeletedAtIsNull(sectionId);
        if (containsAnotherSection
                || activeMilestoneCount != milestones.size()
                || activeMilestoneCount != distinctMilestoneIdCount) {
            throw new MilestoneConcurrentlyModifiedException();
        }
    }

    private void validateExistingEntities(
            List<Milestone> milestones,
            Map<Long, MilestoneJpaEntity> existingEntities
    ) {
        for (Milestone milestone : milestones) {
            if (milestone.getId() != null && !existingEntities.containsKey(milestone.getId())) {
                throw new MilestoneNotFoundException(milestone.getId());
            }
        }
    }

    private boolean hasWeekNumberChanged(
            Milestone milestone,
            Map<Long, MilestoneJpaEntity> existingEntities
    ) {
        MilestoneJpaEntity entity = existingEntities.get(milestone.getId());
        return entity != null && entity.getWeekNumber() != milestone.getWeekNumber();
    }

    private Map<Long, MilestoneJpaEntity> findExistingEntities(List<Milestone> milestones) {
        List<Long> existingIds = milestones.stream()
                .map(Milestone::getId)
                .filter(id -> id != null)
                .toList();
        if (existingIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, MilestoneJpaEntity> entitiesById = new HashMap<>();
        for (MilestoneJpaEntity entity : jpaMilestoneRepository.findAllById(existingIds)) {
            if (entity.getDeletedAt() == null) {
                entitiesById.put(entity.getId(), entity);
            }
        }
        return entitiesById;
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

    private MilestoneJpaEntity entityForSave(
            Milestone milestone,
            Map<Long, MilestoneJpaEntity> existingEntities
    ) {
        if (milestone.getId() == null) {
            return MilestoneJpaEntity.fromDomain(milestone);
        }

        return updateExistingEntity(milestone, existingEntities.get(milestone.getId()));
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
