package kgu.developers.domain.midreport.infrastructure;

import jakarta.persistence.EntityManager;
import java.util.Objects;
import java.util.Optional;
import kgu.developers.domain.midreport.domain.MidReport;
import kgu.developers.domain.midreport.domain.MidReportRepository;
import kgu.developers.domain.midreport.exception.MidReportNotFoundException;
import kgu.developers.domain.midreport.exception.MidReportVersionConflictException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MidReportRepositoryImpl implements MidReportRepository {
    private final JpaMidReportRepository jpaMidReportRepository;
    private final EntityManager entityManager;

    @Override
    @Transactional
    public MidReport save(MidReport midReport) {
        try {
            MidReportJpaEntity entity;
            if (midReport.getId() == null) {
                entity = MidReportJpaEntity.fromDomain(midReport);
            } else {
                entity = entityManager.find(MidReportJpaEntity.class, midReport.getId());
                if (entity == null || entity.getDeletedAt() != null) {
                    throw new MidReportNotFoundException();
                }
                if (!Objects.equals(entity.getVersion(), midReport.getVersion())) {
                    throw new MidReportVersionConflictException();
                }
                entity.updateFromDomain(midReport);
            }
            return jpaMidReportRepository.saveAndFlush(entity).toDomain();
        } catch (OptimisticLockingFailureException exception) {
            throw new MidReportVersionConflictException(exception);
        }
    }

    @Override
    public Optional<MidReport> findById(Long id) {
        return jpaMidReportRepository.findActiveById(id).map(MidReportJpaEntity::toDomain);
    }

    @Override
    public Optional<MidReport> findByTeamIdAndMilestoneId(Long teamId, Long milestoneId) {
        return jpaMidReportRepository.findActiveByTeamIdAndMilestoneId(teamId, milestoneId)
            .map(MidReportJpaEntity::toDomain);
    }
}
