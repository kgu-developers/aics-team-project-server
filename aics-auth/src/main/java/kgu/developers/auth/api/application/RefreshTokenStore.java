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

    public boolean replace(String studentNumber, String expected, String replacement) {
        RefreshTokenJpaEntity stored = refreshTokenRepository.findById(studentNumber).orElse(null);
        if (stored == null || !stored.getToken().equals(expected)) {
            return false;
        }

        stored.updateToken(replacement);
        return true;
    }

    public void delete(String studentNumber) {
        refreshTokenRepository.deleteById(studentNumber);
    }
}
