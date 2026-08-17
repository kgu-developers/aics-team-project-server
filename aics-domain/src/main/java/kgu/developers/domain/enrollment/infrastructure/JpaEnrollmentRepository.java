package kgu.developers.domain.enrollment.infrastructure;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaEnrollmentRepository extends JpaRepository<EnrollmentJpaEntity, Long> {
	Optional<EnrollmentJpaEntity> findByIdAndDeletedAtIsNull(Long id);

	boolean existsBySectionIdAndUserIdAndDeletedAtIsNull(Long sectionId, String userId);

	Optional<EnrollmentJpaEntity> findBySectionIdAndUserIdAndDeletedAtIsNull(Long sectionId, String userId);

	List<EnrollmentJpaEntity> findAllBySectionIdAndDeletedAtIsNullOrderByUserIdAsc(Long sectionId);

	List<EnrollmentJpaEntity> findAllByUserIdAndDeletedAtIsNullOrderBySectionIdAsc(String userId);
}
