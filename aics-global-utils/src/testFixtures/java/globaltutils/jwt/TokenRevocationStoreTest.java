package globaltutils.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import kgu.developers.globalutils.jwt.JwtUtil;
import kgu.developers.globalutils.jwt.TokenRevocationStore;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TokenRevocationStoreTest {

  private static final String STUDENT_NUMBER = "202699999";
  private static final Duration ACCESS_TOKEN_VALIDITY = Duration.ofMinutes(30);
  private static final Duration REFRESH_TOKEN_VALIDITY = Duration.ofDays(14);

  @Mock
  private RedisTemplate<String, String> redisTemplate;

  @Mock
  private ValueOperations<String, String> valueOperations;

  private TokenRevocationStore store;

  @BeforeEach
  void setUp() {
    given(redisTemplate.opsForValue()).willReturn(valueOperations);
    JwtUtil jwtUtil = new JwtUtil("local-dev-jwt-secret-key-0123456789", "issuer",
        ACCESS_TOKEN_VALIDITY, REFRESH_TOKEN_VALIDITY);
    store = new TokenRevocationStore(redisTemplate, jwtUtil);
  }

  @Test
  @DisplayName("무효화 표식은 refreshToken 수명만큼 남는다 (그동안은 재발급도 막아야 하므로)")
  void revokeExpiresWithRefreshToken() {
    store.revokeTokensIssuedBefore(STUDENT_NUMBER);

    verify(valueOperations).set(
        org.mockito.ArgumentMatchers.eq("revoked:" + STUDENT_NUMBER),
        org.mockito.ArgumentMatchers.anyString(),
        org.mockito.ArgumentMatchers.eq(REFRESH_TOKEN_VALIDITY));
  }

  @Test
  @DisplayName("표식이 없으면 무효화되지 않은 것으로 본다")
  void notRevokedWithoutMarker() {
    given(valueOperations.get("revoked:" + STUDENT_NUMBER)).willReturn(null);

    assertThat(store.isRevoked(STUDENT_NUMBER, System.currentTimeMillis())).isFalse();
  }

  private static final long REVOKED_AT = 1_786_201_858_698L;

  private void markerAt(long millis) {
    given(valueOperations.get("revoked:" + STUDENT_NUMBER)).willReturn(String.valueOf(millis));
  }

  @Test
  @DisplayName("앞선 초에 발급된 토큰은 무효다")
  void revokesTokenIssuedBeforeMarker() {
    markerAt(REVOKED_AT);

    assertThat(store.isRevoked(STUDENT_NUMBER, 1_786_201_857_000L)).isTrue();
  }

  @Test
  @DisplayName("같은 초라도 무효화보다 먼저 발급된 토큰은 무효다")
  void keepsTokenIssuedInSameSecond() {
    markerAt(REVOKED_AT);

    assertThat(store.isRevoked(STUDENT_NUMBER, 1_786_201_858_000L)).isTrue();
    assertThat(store.isRevoked(STUDENT_NUMBER, REVOKED_AT - 1)).isTrue();
  }

  @Test
  @DisplayName("같은 초라도 무효화 뒤에 발급된 토큰은 살아 있다 (비밀번호 변경 직후 재로그인)")
  void keepsTokenIssuedAfterMarkerInSameSecond() {
    markerAt(REVOKED_AT);

    assertThat(store.isRevoked(STUDENT_NUMBER, REVOKED_AT)).isFalse();
    assertThat(store.isRevoked(STUDENT_NUMBER, 1_786_201_858_799L)).isFalse();
  }

  @Test
  @DisplayName("다음 초에 발급된 토큰은 살아 있다 (강등 뒤 재로그인)")
  void keepsTokenIssuedAfterMarker() {
    markerAt(REVOKED_AT);

    assertThat(store.isRevoked(STUDENT_NUMBER, 1_786_201_859_000L)).isFalse();
  }

  @Test
  @DisplayName("발급 시각이 없는 토큰은 대조할 수 없으므로 무효로 본다")
  void revokesTokenWithoutIssuedAt() {
    markerAt(REVOKED_AT);

    assertThat(store.isRevoked(STUDENT_NUMBER, null)).isTrue();
  }

  @Test
  @DisplayName("Redis에 저장된 값이 손상되면 예외를 던지지 않고 무효로 본다")
  void revokesOnCorruptedMarker() {
    given(valueOperations.get("revoked:" + STUDENT_NUMBER)).willReturn("not-a-number");

    assertThat(store.isRevoked(STUDENT_NUMBER, REVOKED_AT)).isTrue();
  }
}
