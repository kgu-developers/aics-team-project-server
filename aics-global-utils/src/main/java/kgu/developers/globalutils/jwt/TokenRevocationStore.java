package kgu.developers.globalutils.jwt;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

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
			jwtUtil.getAccessTokenValidity());
	}

	public boolean isRevoked(String studentNumber, Long issuedAtMillis) {
		String revokedAt = redisTemplate.opsForValue().get(KEY_PREFIX + studentNumber);
		if (revokedAt == null) {
			return false;
		}
		if (issuedAtMillis == null) {
			return true;
		}
		return issuedAtMillis < Long.parseLong(revokedAt);
	}
}
