package kgu.developers.domain.user.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaUserArchiveRepository extends JpaRepository<UserArchiveJpaEntity, Long> {
}
