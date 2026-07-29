package kgu.developers.domain.course.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;

import kgu.developers.domain.course.domain.Course;
import kgu.developers.domain.course.domain.CourseRepository;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class CourseRepositoryImpl implements CourseRepository {
	private final JpaCourseRepository jpaCourseRepository;

	@Override
	public Course save(Course course) {
		CourseJpaEntity entity = CourseJpaEntity.toEntity(course);
		CourseJpaEntity savedEntity = jpaCourseRepository.save(entity);
		return savedEntity.toDomain();
	}

	@Override
	public Optional<Course> findById(Long id) {
		Optional<CourseJpaEntity> optionalEntity = jpaCourseRepository.findById(id);
		return optionalEntity.map(CourseJpaEntity::toDomain);
	}

	@Override
	public List<Course> findAllOrderByYearAndName() {
		List<CourseJpaEntity> entities = jpaCourseRepository.findAllByOrderByYearAscNameAsc();
		return entities.stream()
				.map(CourseJpaEntity::toDomain)
				.collect(Collectors.toList());
	}

	@Override
	public void deleteById(Long id) {
		jpaCourseRepository.deleteById(id);
	}
}
