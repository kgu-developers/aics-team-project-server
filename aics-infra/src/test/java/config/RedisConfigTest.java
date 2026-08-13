package config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;

import kgu.developers.infra.config.RedisConfig;

class RedisConfigTest {

    @Test
    @DisplayName("공유 RedisTemplate은 트랜잭션 지원이 꺼져 있다 (읽기가 트랜잭션에 물려 큐잉되는 것을 방지)")
    void sharedTemplateHasTransactionSupportDisabled() {
        RedisTemplate<String, String> template = new RedisConfig()
                .redisTemplate(mock(RedisConnectionFactory.class));

        Field field = ReflectionUtils.findField(RedisTemplate.class, "enableTransactionSupport");
        ReflectionUtils.makeAccessible(field);

        assertThat(ReflectionUtils.getField(field, template)).isEqualTo(false);
    }
}
