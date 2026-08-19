package kgu.developers.domain.enrollment.infrastructure;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import kgu.developers.domain.enrollment.domain.Enrollment;
import kgu.developers.domain.enrollment.domain.EnrollmentRepository;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class EnrollmentRepositoryImpl implements EnrollmentRepository {
  private final JpaEnrollmentRepository jpaEnrollmentRepository;

  @Override
  public Enrollment save(Enrollment enrollment) {
    EnrollmentJpaEntity entity = EnrollmentJpaEntity.toEntity(enrollment);
    return jpaEnrollmentRepository.save(entity).toDomain();
  }

  @Override
  public Optional<Enrollment> findById(Long id) {
    return jpaEnrollmentRepository.findByIdAndDeletedAtIsNull(id)
        .map(EnrollmentJpaEntity::toDomain);
  }

  @Override
  public boolean existsBySectionIdAndUserId(Long sectionId, String userId) {
    return jpaEnrollmentRepository.existsBySectionIdAndUserIdAndDeletedAtIsNull(sectionId, userId);
  }

  @Override
  public Optional<Enrollment> findBySectionIdAndUserId(Long sectionId, String userId) {
    return jpaEnrollmentRepository.findBySectionIdAndUserIdAndDeletedAtIsNull(sectionId, userId)
        .map(EnrollmentJpaEntity::toDomain);
  }

  @Override
  public Optional<Enrollment> findIncludingDeleted(Long sectionId, String userId) {
    return jpaEnrollmentRepository.findBySectionIdAndUserId(sectionId, userId)
        .map(EnrollmentJpaEntity::toDomain);
  }

  @Override
  public List<Enrollment> findAllBySectionId(Long sectionId) {
    return jpaEnrollmentRepository.findAllBySectionIdAndDeletedAtIsNullOrderByUserIdAsc(sectionId)
        .stream()
        .map(EnrollmentJpaEntity::toDomain)
        .toList();
  }

  @Override
  public List<Enrollment> findAllByUserId(String userId) {
    return jpaEnrollmentRepository.findAllByUserIdAndDeletedAtIsNullOrderBySectionIdAsc(userId)
        .stream()
        .map(EnrollmentJpaEntity::toDomain)
        .toList();
  }

  @Override
  public void deleteById(Long id) {
    jpaEnrollmentRepository.deleteById(id);
  }
}
