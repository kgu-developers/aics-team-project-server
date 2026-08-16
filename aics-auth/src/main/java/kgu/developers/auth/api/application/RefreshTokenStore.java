package kgu.developers.auth.api.application;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

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
        refreshTokenRepository.upsert(studentNumber, hash(refreshToken));
    }

    public boolean replace(String studentNumber, String expected, String replacement) {
        RefreshTokenJpaEntity stored = findMatching(studentNumber, expected);
        if (stored == null) {
            return false;
        }

        stored.updateTokenHash(hash(replacement));
        return true;
    }

    public boolean deleteIfMatches(String studentNumber, String refreshToken) {
        RefreshTokenJpaEntity stored = findMatching(studentNumber, refreshToken);
        if (stored == null) {
            return false;
        }

        refreshTokenRepository.delete(stored);
        return true;
    }

    private RefreshTokenJpaEntity findMatching(String studentNumber, String refreshToken) {
        RefreshTokenJpaEntity stored = refreshTokenRepository.findById(studentNumber).orElse(null);
        if (stored == null || !MessageDigest.isEqual(stored.getTokenHash().getBytes(StandardCharsets.UTF_8), hash(refreshToken).getBytes(StandardCharsets.UTF_8))) {
            return null;
        }

        return stored;
    }

    private static String hash(String refreshToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(refreshToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256을 지원하지 않는 JVM", e);
        }
    }
}
