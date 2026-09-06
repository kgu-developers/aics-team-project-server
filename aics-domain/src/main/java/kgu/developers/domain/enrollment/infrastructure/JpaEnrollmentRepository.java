package kgu.developers.domain.enrollment.infrastructure;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaEnrollmentRepository extends JpaRepository<EnrollmentJpaEntity, Long> {
	Optional<EnrollmentJpaEntity> findByIdAndDeletedAtIsNull(Long id);

	boolean existsBySectionIdAndUserIdAndDeletedAtIsNull(Long sectionId, String userId);

	Optional<EnrollmentJpaEntity> findBySectionIdAndUserIdAndDeletedAtIsNull(Long sectionId, String userId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select e from EnrollmentJpaEntity e where e.sectionId = :sectionId and e.userId = :userId and e.deletedAt is null")
	Optional<EnrollmentJpaEntity> findBySectionIdAndUserIdForUpdate(@Param("sectionId") Long sectionId, @Param("userId") String userId);

	Optional<EnrollmentJpaEntity> findBySectionIdAndUserId(Long sectionId, String userId);

	List<EnrollmentJpaEntity> findAllBySectionIdAndDeletedAtIsNullOrderByUserIdAsc(Long sectionId);

	List<EnrollmentJpaEntity> findAllByUserIdAndDeletedAtIsNullOrderBySectionIdAsc(String userId);
}
