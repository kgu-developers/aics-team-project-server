package kgu.developers.globalutils.jwt;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

@Component
public class JwtUtil {

	public static final String ROLE = "role";

	public static final String ISSUED_AT_MILLIS = "iat_ms";

	private static final String TOKEN_TYPE = "type";
	private static final String ACCESS = "access";
	private static final String REFRESH = "refresh";

	private static final int MIN_SECRET_KEY_BYTES = 32;

	private final byte[] secretKey;
	private final String issuer;
	@Getter
    private final Duration accessTokenValidity;
	@Getter
    private final Duration refreshTokenValidity;

	public JwtUtil(
		@Value("${jwt.secret_key}") String secretKey,
		@Value("${jwt.issuer}") String issuer,
		@Value("${jwt.access-token-validity:PT30M}") Duration accessTokenValidity,
		@Value("${jwt.refresh-token-validity:P14D}") Duration refreshTokenValidity) {
		byte[] key = secretKey.getBytes(StandardCharsets.UTF_8);
		if (key.length < MIN_SECRET_KEY_BYTES) {
			// 키 값 자체는 절대 남기지 않는다.
			throw new IllegalArgumentException(
				"jwt.secret_key는 UTF-8 기준 최소 %d바이트여야 합니다 (현재 %d바이트)."
					.formatted(MIN_SECRET_KEY_BYTES, key.length));
		}

		this.secretKey = key;
		this.issuer = issuer;
		this.accessTokenValidity = accessTokenValidity;
		this.refreshTokenValidity = refreshTokenValidity;
	}

    public String createAccessToken(String student_number, String role) {
		return createToken(student_number, ACCESS, accessTokenValidity, role);
	}

	public String createRefreshToken(String student_number) {
		return createToken(student_number, REFRESH, refreshTokenValidity, null);
	}

	public String parseRefreshTokenSubject(String token) {
		return parseClaims(token, REFRESH).getSubject();
	}

	public Claims parseAccessTokenClaims(String token) {
		return parseClaims(token, ACCESS);
	}

	private Claims parseClaims(String token, String expectedType) {
		if (token == null || token.isBlank()) {
			throw new JwtException("토큰이 비어 있습니다.");
		}

		Claims claims = Jwts.parser()
			.setSigningKey(secretKey)
			.requireIssuer(issuer)
			.parseClaimsJws(token)
			.getBody();

		if (!expectedType.equals(claims.get(TOKEN_TYPE))) {
			throw new JwtException(expectedType + " 토큰이 아닙니다.");
		}
		return claims;
	}

	private String createToken(String studentNumber, String type, Duration validity, String role) {
		Date now = new Date();
		return Jwts.builder()
			.setSubject(studentNumber)
			.setIssuer(issuer)
			.claim(TOKEN_TYPE, type)
			.claim(ROLE, role)
			.setIssuedAt(now)
			.claim(ISSUED_AT_MILLIS, now.getTime())
			.setExpiration(new Date(now.getTime() + validity.toMillis()))
			.signWith(SignatureAlgorithm.HS256, secretKey)
			.compact();
	}
}
