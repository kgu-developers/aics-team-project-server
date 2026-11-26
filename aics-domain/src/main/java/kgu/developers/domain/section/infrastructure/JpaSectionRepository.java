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

    @EntityGraph(attributePaths = {"course", "professor"})
    @Query("""
            select s from SectionJpaEntity s where s.deletedAt is null and s.course.id = :courseId
            and (s.professor.studentNumber = :studentNumber or exists (
                select u from UserJpaEntity u where u.studentNumber = :studentNumber
                and u.deletedAt is null
                and u.globalRole = kgu.developers.domain.user.domain.UserGlobalRole.SUPER_ADMIN
            )) order by s.code asc
            """)
    List<SectionJpaEntity> findAllByCourseIdAndProfessorStudentNumberAndDeletedAtIsNullOrderByCodeAsc(
            Long courseId, String studentNumber);

    // 기존 함수명은 유지하되 슈퍼 관리자는 담당 교수와 관계없이 활성 분반에 접근한다.
    @EntityGraph(attributePaths = {"course", "professor"})
    @Query("""
            select s from SectionJpaEntity s where s.deletedAt is null
            and (s.professor.studentNumber = :studentNumber or exists (
                select u from UserJpaEntity u where u.studentNumber = :studentNumber
                and u.deletedAt is null
                and u.globalRole = kgu.developers.domain.user.domain.UserGlobalRole.SUPER_ADMIN
            )) order by s.code asc
            """)
    List<SectionJpaEntity> findAllByProfessorStudentNumberAndDeletedAtIsNullOrderByCodeAsc(String studentNumber);

    @EntityGraph(attributePaths = {"course", "professor"})
    List<SectionJpaEntity> findAllByIdInAndDeletedAtIsNullOrderByCodeAsc(List<Long> ids);

    @Query("""
            select (count(s) > 0) from SectionJpaEntity s where s.id = :id and s.deletedAt is null
            and (s.professor.studentNumber = :studentNumber or exists (
                select u from UserJpaEntity u where u.studentNumber = :studentNumber
                and u.deletedAt is null
                and u.globalRole = kgu.developers.domain.user.domain.UserGlobalRole.SUPER_ADMIN
            ))
            """)
    boolean existsByIdAndProfessorStudentNumberAndDeletedAtIsNull(Long id, String studentNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from SectionJpaEntity s where s.id = :id and s.deletedAt is null")
    Optional<SectionJpaEntity> findActiveByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select s from SectionJpaEntity s where s.id = :id and s.deletedAt is null
            and (s.professor.studentNumber = :studentNumber or exists (
                select u from UserJpaEntity u where u.studentNumber = :studentNumber
                and u.deletedAt is null
                and u.globalRole = kgu.developers.domain.user.domain.UserGlobalRole.SUPER_ADMIN
            ))
            """)
    Optional<SectionJpaEntity> findByIdAndProfessorStudentNumberAndDeletedAtIsNull(
            Long id,
            String studentNumber
    );
}
