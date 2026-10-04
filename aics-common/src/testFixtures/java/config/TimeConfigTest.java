package config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.ZoneId;
import kgu.developers.common.config.TimeConfig;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

class TimeConfigTest {
    @Test
    void sharedConfigurationProvidesOneKoreanServiceClock() {
        try (var context = new AnnotationConfigApplicationContext(TimeConfig.class)) {
            assertThat(context.getBeansOfType(Clock.class)).hasSize(1);
            assertThat(context.getBean(Clock.class).getZone()).isEqualTo(ZoneId.of("Asia/Seoul"));
        }
    }
}
