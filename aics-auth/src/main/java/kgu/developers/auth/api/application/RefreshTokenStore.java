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

    // 새 엔티티를 save하면 version이 0이라, 회전을 거친 행과 merge될 때 StaleObjectStateException이 난다.
    // 기존 행이 있으면 그 행을 그대로 갱신한다.
    public void save(String studentNumber, String refreshToken) {
        refreshTokenRepository.findById(studentNumber).ifPresentOrElse(
                stored -> stored.updateToken(refreshToken),
                () -> refreshTokenRepository.save(new RefreshTokenJpaEntity(studentNumber, refreshToken)));
    }

    // 보관 중인 토큰이 expected와 다르면 false. 같더라도 커밋 시점에 @Version이 어긋나면
    // OptimisticLockingFailureException으로 터지므로, 호출부가 그것도 무효 토큰으로 받는다.
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
