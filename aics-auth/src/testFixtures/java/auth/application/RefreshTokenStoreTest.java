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
import org.mockito.ArgumentCaptor;
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
  @DisplayName("save는 원문이 아니라 해시를 저장한다")
  void saveStoresHashNotRawToken() {
    given(refreshTokenRepository.findById(STUDENT_NUMBER)).willReturn(Optional.empty());

    refreshTokenStore.save(STUDENT_NUMBER, TOKEN);

    ArgumentCaptor<RefreshTokenJpaEntity> captor =
        ArgumentCaptor.forClass(RefreshTokenJpaEntity.class);
    verify(refreshTokenRepository).save(captor.capture());
    assertThat(captor.getValue().getTokenHash()).isNotEqualTo(TOKEN).hasSize(64);
  }

  @Test
  @DisplayName("save는 기존 행이 있으면 새 엔티티를 만들지 않고 그 행을 갱신한다 (@Version 충돌 방지)")
  void saveUpdatesStoredRow() {
    RefreshTokenJpaEntity stored = new RefreshTokenJpaEntity(STUDENT_NUMBER, "old-hash");
    given(refreshTokenRepository.findById(STUDENT_NUMBER)).willReturn(Optional.of(stored));

    refreshTokenStore.save(STUDENT_NUMBER, TOKEN);

    assertThat(stored.getTokenHash()).isNotEqualTo("old-hash").hasSize(64);
    // version 0짜리 새 엔티티를 merge하면 회전을 거친 행과 충돌한다.
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
}
