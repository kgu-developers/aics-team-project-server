package kgu.developers.domain.enrollment.domain;

import java.util.List;
import java.util.Optional;

public interface EnrollmentRepository {
  Enrollment save(Enrollment enrollment);

  Optional<Enrollment> findById(Long id);

  List<Enrollment> findAllBySectionId(Long sectionId);

  List<Enrollment> findAllByStudentNumber(String studentNumber);

  void deleteById(Long id);
}
