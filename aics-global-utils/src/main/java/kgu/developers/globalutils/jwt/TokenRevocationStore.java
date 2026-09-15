package kgu.developers.globalutils.jwt;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class TokenRevocationStore {

	private static final String KEY_PREFIX = "revoked:";
	private static final String PASSWORD_CHANGE_KEY_PREFIX = "password-change-required:";

	private final RedisTemplate<String, String> redisTemplate;
	private final JwtUtil jwtUtil;

	public void revokeTokensIssuedBefore(String studentNumber) {
		redisTemplate.opsForValue().set(
			KEY_PREFIX + studentNumber,
			String.valueOf(System.currentTimeMillis()),
			jwtUtil.getRefreshTokenValidity());
	}

	public boolean isRevoked(String studentNumber, Long issuedAtMillis) {
		String revokedAt = redisTemplate.opsForValue().get(KEY_PREFIX + studentNumber);
		if (revokedAt == null) {
			return false;
		}
		if (issuedAtMillis == null) {
			return true;
		}
		long revokedMillis;
		try {
			revokedMillis = Long.parseLong(revokedAt);
		} catch (NumberFormatException e) {
			log.warn("Redis에 저장된 revokedAt 값이 손상되어 파싱할 수 없습니다.");
			return true;
		}
		if (revokedMillis <= 0) {
			return true;
		}
		return issuedAtMillis < revokedMillis;
	}

	public void requirePasswordChangeUntil(String studentNumber, LocalDateTime expiresAt) {
		redisTemplate.opsForValue().set(PASSWORD_CHANGE_KEY_PREFIX + studentNumber,
			String.valueOf(expiresAt.toInstant(ZoneOffset.UTC).toEpochMilli()));
	}

	public Optional<LocalDateTime> passwordChangeExpiresAt(String studentNumber) {
		String expiresAt = redisTemplate.opsForValue().get(PASSWORD_CHANGE_KEY_PREFIX + studentNumber);
		if (expiresAt == null) {
			return Optional.empty();
		}
		try {
			return Optional.of(LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(Long.parseLong(expiresAt)), ZoneOffset.UTC));
		} catch (NumberFormatException e) {
			return Optional.of(LocalDateTime.MIN);
		}
	}

	public void clearPasswordChangeRequirement(String studentNumber) {
		redisTemplate.delete(PASSWORD_CHANGE_KEY_PREFIX + studentNumber);
	}
}
