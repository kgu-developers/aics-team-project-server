package kgu.developers.domain.submission.infrastructure;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaSubmissionRepository extends JpaRepository<SubmissionJpaEntity, Long> {
    Optional<SubmissionJpaEntity> findByIdAndDeletedAtIsNull(Long id);

    Optional<SubmissionJpaEntity> findByTeamIdAndMilestoneIdAndDeletedAtIsNull(Long teamId, Long milestoneId);

    List<SubmissionJpaEntity> findAllByMilestoneIdAndDeletedAtIsNull(Long milestoneId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from SubmissionJpaEntity s where s.id = :id and s.deletedAt is null")
    Optional<SubmissionJpaEntity> findByIdForUpdate(@Param("id") Long id);
}
