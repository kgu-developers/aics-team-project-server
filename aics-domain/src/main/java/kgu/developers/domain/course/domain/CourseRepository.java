package kgu.developers.domain.course.domain;

import java.util.List;
import java.util.Optional;

public interface CourseRepository {
  Course save(Course course);

  Optional<Course> findById(Long id);

  List<Course> findAllOrderByYearAndName();

  void deleteById(Long id);
}
