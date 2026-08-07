package kgu.developers.domain.auth.infrastructure;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import jakarta.persistence.LockModeType;

public interface JpaRefreshTokenRepository extends JpaRepository<RefreshTokenJpaEntity, String> {

    @Override
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<RefreshTokenJpaEntity> findById(String studentNumber);
}
