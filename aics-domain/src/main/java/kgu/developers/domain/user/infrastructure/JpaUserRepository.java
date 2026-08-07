package kgu.developers.domain.user.infrastructure;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaUserRepository extends JpaRepository<UserJpaEntity, String> {
    Optional<UserJpaEntity> findByStudentNumberAndDeletedAtIsNull(String student_number);

    List<UserJpaEntity> findAllByDeletedAtIsNullOrderByStudentNumberAsc();
}
