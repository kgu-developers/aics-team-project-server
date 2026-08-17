package kgu.developers.domain.enrollment.domain;

import java.util.List;
import java.util.Optional;

public interface EnrollmentRepository {
  Enrollment save(Enrollment enrollment);

  Optional<Enrollment> findById(Long id);

  boolean existsBySectionIdAndUserId(Long sectionId, String userId);

  Optional<Enrollment> findBySectionIdAndUserId(Long sectionId, String userId);

  List<Enrollment> findAllBySectionId(Long sectionId);

  List<Enrollment> findAllByUserId(String userId);

  void deleteById(Long id);
}
