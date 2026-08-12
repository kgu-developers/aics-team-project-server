package kgu.developers.domain.enrollment.infrastructure;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaEnrollmentRepository extends JpaRepository<EnrollmentJpaEntity, Long> {
	Optional<EnrollmentJpaEntity> findByIdAndDeletedAtIsNull(Long id);

	List<EnrollmentJpaEntity> findAllBySectionIdAndDeletedAtIsNullOrderByStudentNumberAsc(Long sectionId);

	List<EnrollmentJpaEntity> findAllByStudentNumberAndDeletedAtIsNullOrderBySectionIdAsc(String studentNumber);
}
