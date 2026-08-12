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
  public List<Enrollment> findAllBySectionId(Long sectionId) {
    return jpaEnrollmentRepository.findAllBySectionIdAndDeletedAtIsNullOrderByStudentNumberAsc(sectionId)
        .stream()
        .map(EnrollmentJpaEntity::toDomain)
        .toList();
  }

  @Override
  public List<Enrollment> findAllByStudentNumber(String studentNumber) {
    return jpaEnrollmentRepository.findAllByStudentNumberAndDeletedAtIsNullOrderBySectionIdAsc(studentNumber)
        .stream()
        .map(EnrollmentJpaEntity::toDomain)
        .toList();
  }

  @Override
  public void deleteById(Long id) {
    jpaEnrollmentRepository.deleteById(id);
  }
}
