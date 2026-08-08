package kgu.developers.domain.user.infrastructure;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaUserRepository extends JpaRepository<UserJpaEntity, String> {
    boolean existsByEmailAndStudentNumberNot(String email, String studentNumber);

    boolean existsByStudentNumberAndDeletedAtIsNull(String studentNumber);

    Optional<UserJpaEntity> findByStudentNumberAndDeletedAtIsNull(String studentNumber);

    List<UserJpaEntity> findAllByDeletedAtIsNullOrderByStudentNumberAsc();
}
