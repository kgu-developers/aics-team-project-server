package kgu.developers.domain.milestone.infrastructure;

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
        MilestoneJpaEntity savedEntity = jpaMilestoneRepository.save(entityForSave(milestone));
        return savedEntity.toDomain();
    }

    @Override
    @Transactional
    public List<Milestone> saveAll(List<Milestone> milestones) {
        Map<Long, MilestoneJpaEntity> existingEntities = findExistingEntities(milestones);
        List<MilestoneJpaEntity> entities = milestones.stream()
                .map(milestone -> entityForSave(milestone, existingEntities))
                .toList();
        return jpaMilestoneRepository.saveAll(entities).stream()
                .map(MilestoneJpaEntity::toDomain)
                .toList();
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
                .findActiveByIdForUpdate(milestone.getId())
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
    public List<Milestone> findAllBySectionIdOrderByWeekNumber(Long sectionId) {
        return jpaMilestoneRepository
                .findAllBySectionIdAndDeletedAtIsNullOrderByWeekNumberAsc(sectionId)
                .stream()
                .map(MilestoneJpaEntity::toDomain)
                .toList();
    }

    @Override
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
