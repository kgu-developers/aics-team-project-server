package kgu.developers.domain.section.infrastructure;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import jakarta.persistence.LockModeType;

public interface JpaSectionRepository extends JpaRepository<SectionJpaEntity, Long> {
    @EntityGraph(attributePaths = {"course", "professor"})
    Optional<SectionJpaEntity> findByIdAndDeletedAtIsNull(Long id);

    @EntityGraph(attributePaths = {"course", "professor"})
    List<SectionJpaEntity> findAllByCourseIdAndDeletedAtIsNullOrderByCodeAsc(Long courseId);

    // UserJpaEntity의 식별자는 id가 아니라 studentNumber라서 professorId로는 해석되지 않는다
    @EntityGraph(attributePaths = {"course", "professor"})
    List<SectionJpaEntity> findAllByProfessorStudentNumberAndDeletedAtIsNullOrderByCodeAsc(String studentNumber);

    @EntityGraph(attributePaths = {"course", "professor"})
    List<SectionJpaEntity> findAllByIdInAndDeletedAtIsNullOrderByCodeAsc(List<Long> ids);

    boolean existsByIdAndProfessorStudentNumberAndDeletedAtIsNull(Long id, String studentNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<SectionJpaEntity> findByIdAndProfessorStudentNumberAndDeletedAtIsNull(
            Long id,
            String studentNumber
    );
}
