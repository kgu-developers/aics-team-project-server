package kgu.developers.domain.course.domain;

import java.util.List;
import java.util.Optional;

public interface CourseRepository {
  Course save(Course course);

  Optional<Course> findById(Long id);

  List<Course> findAllOwnedByProfessor(String professorId);

  void deleteById(Long id);
}
