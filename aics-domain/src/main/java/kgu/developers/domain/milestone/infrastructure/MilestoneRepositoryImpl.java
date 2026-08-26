package kgu.developers.domain.milestone.infrastructure;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
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

    @Override
    @Transactional
    public Milestone save(Milestone milestone) {
        MilestoneJpaEntity entity = entityForSave(milestone);
        MilestoneJpaEntity savedEntity = milestone.getId() == null
                ? jpaMilestoneRepository.saveAndFlush(entity)
                : jpaMilestoneRepository.save(entity);
        return savedEntity.toDomain();
    }

    @Override
    @Transactional
    public List<Milestone> saveAll(List<Milestone> milestones) {
        Map<Long, MilestoneJpaEntity> existingEntities = findExistingEntities(milestones);
        validateExistingEntities(milestones, existingEntities);
        moveWeekNumberChangesToTemporaryRange(milestones, existingEntities);
        List<MilestoneJpaEntity> entities = milestones.stream()
                .map(milestone -> entityForSave(milestone, existingEntities))
                .toList();
        return jpaMilestoneRepository.saveAllAndFlush(entities).stream()
                .map(MilestoneJpaEntity::toDomain)
                .toList();
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

    private void moveWeekNumberChangesToTemporaryRange(
            List<Milestone> milestones,
            Map<Long, MilestoneJpaEntity> existingEntities
    ) {
        int highestReservedWeekNumber = milestones.stream()
                .mapToInt(Milestone::getWeekNumber)
                .max()
                .orElse(0);
        for (MilestoneJpaEntity entity : existingEntities.values()) {
            highestReservedWeekNumber = Math.max(highestReservedWeekNumber, entity.getWeekNumber());
        }

        List<MilestoneJpaEntity> changedEntities = new ArrayList<>();
        for (Milestone milestone : milestones) {
            MilestoneJpaEntity entity = existingEntities.get(milestone.getId());
            if (entity != null && entity.getWeekNumber() != milestone.getWeekNumber()) {
                changedEntities.add(entity);
            }
        }

        for (int index = 0; index < changedEntities.size(); index++) {
            int temporaryWeekNumber = Math.addExact(highestReservedWeekNumber, index + 1);
            changedEntities.get(index).moveToTemporaryWeekNumber(temporaryWeekNumber);
        }
        if (!changedEntities.isEmpty()) {
            jpaMilestoneRepository.flush();
        }
    }

    private Map<Long, MilestoneJpaEntity> findExistingEntities(List<Milestone> milestones) {
        List<Long> existingIds = milestones.stream()
                .map(Milestone::getId)
                .filter(id -> id != null)
                .toList();
        if (existingIds.isEmpty()) {
            return Map.of();
        }

        return jpaMilestoneRepository.findAllActiveByIdInForUpdate(existingIds).stream()
                .collect(Collectors.toMap(MilestoneJpaEntity::getId, entity -> entity));
    }

    private MilestoneJpaEntity entityForSave(Milestone milestone) {
        if (milestone.getId() == null) {
            return MilestoneJpaEntity.fromDomain(milestone);
        }

        MilestoneJpaEntity entity = jpaMilestoneRepository
                .findById(milestone.getId())
                .filter(existingEntity -> existingEntity.getDeletedAt() == null)
                .orElse(null);
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
    @Transactional
    public Optional<Milestone> findByIdForUpdate(Long id) {
        return jpaMilestoneRepository.findActiveByIdForUpdate(id)
                .map(MilestoneJpaEntity::toDomain);
    }

    @Override
    @Transactional
    public Optional<Milestone> findByIdAndSectionIdForUpdate(Long id, Long sectionId) {
        return jpaMilestoneRepository.findActiveByIdAndSectionIdForUpdate(id, sectionId)
                .map(MilestoneJpaEntity::toDomain);
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
    @Transactional
    public List<Milestone> findAllBySectionIdForUpdateOrderByWeekNumber(Long sectionId) {
        return jpaMilestoneRepository.findAllActiveBySectionIdForUpdate(sectionId)
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
