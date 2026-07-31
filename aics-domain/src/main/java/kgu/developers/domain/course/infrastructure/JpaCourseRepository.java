package kgu.developers.domain.course.infrastructure;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaCourseRepository extends JpaRepository<CourseJpaEntity, Long> {
	List<CourseJpaEntity> findAllByOrderByYearAscNameAsc();
}
