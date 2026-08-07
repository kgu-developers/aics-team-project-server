package globaltutils.jwt;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.Date;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.SignatureException;
import kgu.developers.globalutils.jwt.JwtUtil;

class JwtUtilTest {

  private static final String SECRET = "local-dev-jwt-secret-key-0123456789";
  private static final String ISSUER = "kgudevelopers@gmail.com";
  private static final String STUDENT_NUMBER = "202699999";

  private final JwtUtil jwtUtil =
      new JwtUtil(SECRET, ISSUER, Duration.ofMinutes(30), Duration.ofDays(14));

  private Claims claimsOf(String token) {
    return Jwts.parser().setSigningKey(SECRET.getBytes(UTF_8)).parseClaimsJws(token).getBody();
  }

  @Test
  @DisplayName("accessToken은 학번을 subject로, 발급자와 만료를 담는다")
  void createAccessToken() {
    Claims claims = claimsOf(jwtUtil.createAccessToken(STUDENT_NUMBER, "STUDENT"));

    assertThat(claims.getSubject()).isEqualTo(STUDENT_NUMBER);
    assertThat(claims.getIssuer()).isEqualTo(ISSUER);
    assertThat(claims.get("type")).isEqualTo("access");
    assertThat(claims.get("role")).isEqualTo("STUDENT");
    assertThat(claims.getExpiration()).isAfter(new Date());
  }

  @Test
  @DisplayName("parseAccessTokenClaims는 accessToken에서 학번과 역할을 꺼낸다")
  void parseAccessTokenClaims() {
    Claims claims = jwtUtil.parseAccessTokenClaims(jwtUtil.createAccessToken(STUDENT_NUMBER, "ADMIN"));

    assertThat(claims.getSubject()).isEqualTo(STUDENT_NUMBER);
    assertThat(claims.get("role", String.class)).isEqualTo("ADMIN");
  }

  @Test
  @DisplayName("parseAccessTokenClaims는 refreshToken을 넘기면 거부한다")
  void parseAccessTokenClaimsRejectsRefreshToken() {
    String refreshToken = jwtUtil.createRefreshToken(STUDENT_NUMBER);

    assertThatThrownBy(() -> jwtUtil.parseAccessTokenClaims(refreshToken))
        .isInstanceOf(JwtException.class);
  }

  @Test
  @DisplayName("refreshToken은 type이 다르고 accessToken보다 늦게 만료된다")
  void createRefreshToken() {
    Claims access = claimsOf(jwtUtil.createAccessToken(STUDENT_NUMBER, "STUDENT"));
    Claims refresh = claimsOf(jwtUtil.createRefreshToken(STUDENT_NUMBER));

    assertThat(refresh.getSubject()).isEqualTo(STUDENT_NUMBER);
    assertThat(refresh.get("type")).isEqualTo("refresh");
    assertThat(refresh.getExpiration()).isAfter(access.getExpiration());
  }

  @Test
  @DisplayName("parseRefreshTokenSubject는 refreshToken에서 학번을 꺼낸다")
  void parseRefreshTokenSubject() {
    String token = jwtUtil.createRefreshToken(STUDENT_NUMBER);

    assertThat(jwtUtil.parseRefreshTokenSubject(token)).isEqualTo(STUDENT_NUMBER);
  }

  @Test
  @DisplayName("parseRefreshTokenSubject는 accessToken을 넘기면 거부한다")
  void parseRefreshTokenSubjectRejectsAccessToken() {
    String accessToken = jwtUtil.createAccessToken(STUDENT_NUMBER, "STUDENT");

    assertThatThrownBy(() -> jwtUtil.parseRefreshTokenSubject(accessToken))
        .isInstanceOf(JwtException.class);
  }

  @Test
  @DisplayName("parseRefreshTokenSubject는 다른 키로 서명된 토큰을 거부한다")
  void parseRefreshTokenSubjectRejectsForeignToken() {
    JwtUtil other = new JwtUtil("another-secret-key-0123456789012345", ISSUER,
        Duration.ofMinutes(30), Duration.ofDays(14));
    String foreign = other.createRefreshToken(STUDENT_NUMBER);

    assertThatThrownBy(() -> jwtUtil.parseRefreshTokenSubject(foreign))
        .isInstanceOf(JwtException.class);
  }

  @Test
  @DisplayName("같은 키로 서명됐어도 발급자가 다르면 거부한다")
  void rejectsTokenFromAnotherIssuer() {
    JwtUtil other = new JwtUtil(SECRET, "evil@example.com", Duration.ofMinutes(30), Duration.ofDays(14));

    assertThatThrownBy(() -> jwtUtil.parseAccessTokenClaims(other.createAccessToken(STUDENT_NUMBER, "ADMIN")))
        .isInstanceOf(JwtException.class);
    assertThatThrownBy(() -> jwtUtil.parseRefreshTokenSubject(other.createRefreshToken(STUDENT_NUMBER)))
        .isInstanceOf(JwtException.class);
  }

  @Test
  @DisplayName("발급자 클레임이 아예 없는 토큰도 거부한다")
  void rejectsTokenWithoutIssuer() {
    String noIssuer = Jwts.builder()
        .setSubject(STUDENT_NUMBER)
        .claim("type", "access")
        .setExpiration(new Date(System.currentTimeMillis() + 60_000))
        .signWith(SignatureAlgorithm.HS256, SECRET.getBytes(UTF_8))
        .compact();

    assertThatThrownBy(() -> jwtUtil.parseAccessTokenClaims(noIssuer))
        .isInstanceOf(JwtException.class);
  }

  @Test
  @DisplayName("parseRefreshTokenSubject는 만료된 토큰을 거부한다")
  void parseRefreshTokenSubjectRejectsExpiredToken() {
    JwtUtil expired = new JwtUtil(SECRET, ISSUER, Duration.ofMinutes(30), Duration.ofSeconds(-1));
    String token = expired.createRefreshToken(STUDENT_NUMBER);

    assertThatThrownBy(() -> jwtUtil.parseRefreshTokenSubject(token))
        .isInstanceOf(JwtException.class);
  }

  @Test
  @DisplayName("다른 키로 서명을 검증하면 실패한다")
  void rejectsTokenSignedWithAnotherKey() {
    String token = jwtUtil.createAccessToken(STUDENT_NUMBER, "STUDENT");

    assertThatThrownBy(() -> Jwts.parser().setSigningKey("another-key-0123456789012345".getBytes(UTF_8))
        .parseClaimsJws(token))
        .isInstanceOf(SignatureException.class);
  }
}
