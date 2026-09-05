package kgu.developers.domain.project.infrastructure;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface JpaProjectRepository extends JpaRepository<ProjectJpaEntity, Long> {
    Optional<ProjectJpaEntity> findByIdAndDeletedAtIsNull(Long id);

    // 제안 완료·동의 경로의 read-modify-write 경쟁 방지: 조회 시점에 행을 잠근다 (호출자 트랜잭션 필수)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from ProjectJpaEntity p where p.id = :id and p.deletedAt is null")
    Optional<ProjectJpaEntity> findByIdForUpdate(@Param("id") Long id);

    List<ProjectJpaEntity> findAllByTeamIdAndDeletedAtIsNull(Long teamId);

    Optional<ProjectJpaEntity> findByTeamId(Long teamId);
}
