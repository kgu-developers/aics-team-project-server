package kgu.developers.domain.section.infrastructure;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
    @Query("select s from SectionJpaEntity s where s.id = :id and s.deletedAt is null")
    Optional<SectionJpaEntity> findActiveByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<SectionJpaEntity> findByIdAndProfessorStudentNumberAndDeletedAtIsNull(
            Long id,
            String studentNumber
    );
}
