package kgu.developers.domain.course.infrastructure;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaCourseRepository extends JpaRepository<CourseJpaEntity, Long> {
	Optional<CourseJpaEntity> findByIdAndDeletedAtIsNull(Long id);

	List<CourseJpaEntity> findAllByDeletedAtIsNullOrderByYearAscNameAsc();
}
