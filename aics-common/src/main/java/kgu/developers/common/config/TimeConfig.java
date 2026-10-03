package kgu.developers.common.config;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// 저장된 시각이 KST 벽시계 기준이라, JVM 기본 시간대(배포 컨테이너는 UTC)로 "오늘"을 구하면
// 하루가 어긋난다. 서비스 시간대를 쓰는 모듈이 모두 이 빈 하나를 공유한다.
@Configuration
public class TimeConfig {
    private static final ZoneId SERVICE_ZONE = ZoneId.of("Asia/Seoul");

    @Bean
    public Clock serviceClock() {
        return Clock.system(SERVICE_ZONE);
    }
}
