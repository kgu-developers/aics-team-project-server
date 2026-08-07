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

  @Test
  @DisplayName("save는 기존 행이 있으면 새 엔티티를 만들지 않고 그 행을 갱신한다 (@Version 충돌 방지)")
  void saveUpdatesStoredRow() {
    RefreshTokenJpaEntity stored = new RefreshTokenJpaEntity(STUDENT_NUMBER, "old-refresh-token");
    given(refreshTokenRepository.findById(STUDENT_NUMBER)).willReturn(Optional.of(stored));

    refreshTokenStore.save(STUDENT_NUMBER, "new-refresh-token");

    assertThat(stored.getToken()).isEqualTo("new-refresh-token");
    // version 0짜리 새 엔티티를 merge하면 회전을 거친 행과 충돌한다.
    verify(refreshTokenRepository, never()).save(any());
  }

  @Test
  @DisplayName("save는 기존 행이 없으면 새로 저장한다")
  void saveInsertsWhenAbsent() {
    given(refreshTokenRepository.findById(STUDENT_NUMBER)).willReturn(Optional.empty());

    refreshTokenStore.save(STUDENT_NUMBER, "refresh-token");

    verify(refreshTokenRepository).save(any(RefreshTokenJpaEntity.class));
  }
}
