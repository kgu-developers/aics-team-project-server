package kgu.developers.auth.api.application;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import kgu.developers.domain.auth.infrastructure.JpaRefreshTokenRepository;
import kgu.developers.domain.auth.infrastructure.RefreshTokenJpaEntity;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@Transactional
public class RefreshTokenStore {

    private final JpaRefreshTokenRepository refreshTokenRepository;

    public void save(String studentNumber, String refreshToken) {
        refreshTokenRepository.save(new RefreshTokenJpaEntity(studentNumber, refreshToken));
    }

    public String consume(String studentNumber) {
        String token = refreshTokenRepository.findById(studentNumber)
            .map(RefreshTokenJpaEntity::getToken)
            .orElse(null);
        delete(studentNumber);
        return token;
    }

    public void delete(String studentNumber) {
        refreshTokenRepository.deleteById(studentNumber);
    }
}
