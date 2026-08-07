package kgu.developers.domain.milestone.infrastructure;

import java.util.List;
import java.util.Optional;
import kgu.developers.domain.milestone.domain.Milestone;
import kgu.developers.domain.milestone.domain.MilestoneRepository;
import kgu.developers.domain.milestone.domain.MilestoneStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class MilestoneRepositoryImpl implements MilestoneRepository {

    private final JpaMilestoneRepository jpaMilestoneRepository;

    @Override
    public Milestone save(Milestone milestone) {
        MilestoneJpaEntity savedEntity = jpaMilestoneRepository.save(entityForSave(milestone));
        return savedEntity.toDomain();
    }

    @Override
    public List<Milestone> saveAll(List<Milestone> milestones) {
        List<MilestoneJpaEntity> entities = milestones.stream()
                .map(this::entityForSave)
                .toList();
        return jpaMilestoneRepository.saveAll(entities).stream()
                .map(MilestoneJpaEntity::toDomain)
                .toList();
    }

    private MilestoneJpaEntity entityForSave(Milestone milestone) {
        if (milestone.getId() == null) {
            return MilestoneJpaEntity.fromDomain(milestone);
        }

        MilestoneJpaEntity entity = jpaMilestoneRepository
                .findByIdAndDeletedAtIsNull(milestone.getId())
                .orElseThrow(() -> new IllegalStateException(
                        "갱신할 마일스톤을 찾을 수 없습니다. id=" + milestone.getId()
                ));
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
