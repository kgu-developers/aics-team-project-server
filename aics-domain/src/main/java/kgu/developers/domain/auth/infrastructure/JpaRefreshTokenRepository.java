package kgu.developers.domain.auth.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaRefreshTokenRepository extends JpaRepository<RefreshTokenJpaEntity, String> {
}
