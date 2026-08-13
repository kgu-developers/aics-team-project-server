package kgu.developers.globalutils.jwt;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class TokenRevocationStore {

	private static final String KEY_PREFIX = "revoked:";

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
			log.warn("Redis에 저장된 revokedAt 값이 손상되어 파싱할 수 없습니다. studentNumber={}, value={}",
				studentNumber, revokedAt);
			return true;
		}
		return issuedAtMillis < revokedMillis;
	}
}
