package user.infrastructure;

import static kgu.developers.domain.user.domain.UserGlobalRole.USER;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import kgu.developers.domain.user.domain.User;
import kgu.developers.domain.user.infrastructure.UserJpaEntity;

class UserJpaEntityTest {

    @Test
    @DisplayName("UserJpaEntity는 마지막 로그인 시각을 양방향 변환한다")
    void mapsLastLoginAt() {
        LocalDateTime lastLoginAt = LocalDateTime.of(2026, 9, 1, 10, 30);
        User user = User.builder()
                .studentNumber("202699999")
                .email("kgu@kyonggi.ac.kr")
                .name("김철수")
                .password("hashed")
                .globalRole(USER)
                .phone("010-1234-6789")
                .lastLoginAt(lastLoginAt)
                .build();

        User mapped = UserJpaEntity.toEntity(user).toDomain();

        assertThat(mapped.getLastLoginAt()).isEqualTo(lastLoginAt);
    }
}
