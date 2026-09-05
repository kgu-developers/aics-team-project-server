package kgu.developers.domain.enrollment.domain;

import java.util.List;
import java.util.Optional;

public interface EnrollmentRepository {
  Enrollment save(Enrollment enrollment);

  Optional<Enrollment> findById(Long id);

  boolean existsBySectionIdAndUserId(Long sectionId, String userId);

  Optional<Enrollment> findBySectionIdAndUserId(Long sectionId, String userId);

  /** 사전조사 응답처럼 "존재하지 않을 수도 있는 다른 엔티티"의 중복 생성을 막을 때, 이미
   *  존재하는 이 Enrollment 행을 먼저 잠가 같은 사용자·분반 조합의 동시 요청을 직렬화하는 용도. */
  Optional<Enrollment> findBySectionIdAndUserIdForUpdate(Long sectionId, String userId);

  Optional<Enrollment> findIncludingDeleted(Long sectionId, String userId);

  List<Enrollment> findAllBySectionId(Long sectionId);

  List<Enrollment> findAllByUserId(String userId);

  void deleteById(Long id);
}
