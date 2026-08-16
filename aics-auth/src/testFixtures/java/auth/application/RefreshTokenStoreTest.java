package auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import kgu.developers.auth.api.application.RefreshTokenStore;
import kgu.developers.domain.auth.infrastructure.JpaRefreshTokenRepository;
import kgu.developers.domain.auth.infrastructure.RefreshTokenJpaEntity;

@ExtendWith(MockitoExtension.class)
class RefreshTokenStoreTest {

  @Mock
  private JpaRefreshTokenRepository refreshTokenRepository;

  @InjectMocks
  private RefreshTokenStore refreshTokenStore;

  private static final String STUDENT_NUMBER = "202699999";

  // "refresh-token"의 SHA-256
  private static final String TOKEN = "refresh-token";
  private static final String TOKEN_HASH =
      "0eb17643d4e9261163783a420859c92c7d212fa9624106a12b510afbec266120";

  @Test
  @DisplayName("save는 원문이 아니라 해시를 upsert한다")
  void saveStoresHashNotRawToken() {
    refreshTokenStore.save(STUDENT_NUMBER, TOKEN);

    verify(refreshTokenRepository).upsert(STUDENT_NUMBER, TOKEN_HASH);
    assertThat(TOKEN_HASH).isNotEqualTo(TOKEN).hasSize(64);
  }

  @Test
  @DisplayName("save는 조회 후 insert로 나누지 않는다 (동시 로그인 PK 충돌 방지)")
  void saveDoesNotReadThenInsert() {
    refreshTokenStore.save(STUDENT_NUMBER, TOKEN);

    verify(refreshTokenRepository, never()).findById(any());
    verify(refreshTokenRepository, never()).save(any());
  }

  @Test
  @DisplayName("replace는 원문을 해시해 보관 중인 값과 대조한다")
  void replaceComparesHash() {
    RefreshTokenJpaEntity stored = new RefreshTokenJpaEntity(STUDENT_NUMBER, TOKEN_HASH);
    given(refreshTokenRepository.findById(STUDENT_NUMBER)).willReturn(Optional.of(stored));

    assertThat(refreshTokenStore.replace(STUDENT_NUMBER, TOKEN, "new-refresh-token")).isTrue();
    assertThat(stored.getTokenHash()).isNotEqualTo(TOKEN_HASH).hasSize(64);
  }

  @Test
  @DisplayName("replace는 원문이 다르면 false를 돌려주고 보관 값을 바꾸지 않는다")
  void replaceRejectsOtherToken() {
    RefreshTokenJpaEntity stored = new RefreshTokenJpaEntity(STUDENT_NUMBER, TOKEN_HASH);
    given(refreshTokenRepository.findById(STUDENT_NUMBER)).willReturn(Optional.of(stored));

    assertThat(refreshTokenStore.replace(STUDENT_NUMBER, "other-token", "new-refresh-token"))
        .isFalse();
    assertThat(stored.getTokenHash()).isEqualTo(TOKEN_HASH);
  }

  @Test
  @DisplayName("deleteIfMatches는 제시된 토큰이 보관 값과 같을 때만 지운다")
  void deleteIfMatchesDeletesCurrentToken() {
    RefreshTokenJpaEntity stored = new RefreshTokenJpaEntity(STUDENT_NUMBER, TOKEN_HASH);
    given(refreshTokenRepository.findById(STUDENT_NUMBER)).willReturn(Optional.of(stored));

    assertThat(refreshTokenStore.deleteIfMatches(STUDENT_NUMBER, TOKEN)).isTrue();

    verify(refreshTokenRepository).delete(stored);
  }

  @Test
  @DisplayName("deleteIfMatches는 회전된 옛 토큰으로는 활성 세션을 끊지 못한다")
  void deleteIfMatchesRejectsRotatedToken() {
    RefreshTokenJpaEntity stored = new RefreshTokenJpaEntity(STUDENT_NUMBER, TOKEN_HASH);
    given(refreshTokenRepository.findById(STUDENT_NUMBER)).willReturn(Optional.of(stored));

    assertThat(refreshTokenStore.deleteIfMatches(STUDENT_NUMBER, "rotated-out-token")).isFalse();

    verify(refreshTokenRepository, never()).delete(any(RefreshTokenJpaEntity.class));
    verify(refreshTokenRepository, never()).deleteById(any());
  }

  @Test
  @DisplayName("deleteIfMatches는 보관된 토큰이 없으면 false를 돌려준다")
  void deleteIfMatchesWithoutStoredToken() {
    given(refreshTokenRepository.findById(STUDENT_NUMBER)).willReturn(Optional.empty());

    assertThat(refreshTokenStore.deleteIfMatches(STUDENT_NUMBER, TOKEN)).isFalse();

    verify(refreshTokenRepository, never()).delete(any(RefreshTokenJpaEntity.class));
  }
}
