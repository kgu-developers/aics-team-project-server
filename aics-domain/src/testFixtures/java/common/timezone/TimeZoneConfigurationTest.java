package common.timezone;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

// KD3-287 실제 회귀 가드.
//
// 9시간 시차는 "JVM 기본 시간대(UTC)"와 "hibernate.jdbc.time_zone(Asia/Seoul)"이 서로 달라서
// Hibernate가 저장 시 +9, 조회 시 -9를 적용해 생긴 문제였다. 해결은 두 가지였다.
//   1) 컨테이너 시간대를 KST로 고정  → Dockerfile ENV TZ
//   2) 더는 필요 없어진 변환 설정 제거 → application.yml의 jdbc.time_zone
// 이 테스트는 그 두 조건이 설정 파일에 그대로 남아 있는지를 직접 읽어 확인한다.
// 둘 중 하나라도 되돌아가면 여기서 깨진다.
class TimeZoneConfigurationTest {

    private static final List<String> MODULES = List.of("aics-api", "aics-admin", "aics-auth");

    @Test
    @DisplayName("어느 모듈 설정에도 hibernate.jdbc.time_zone이 남아 있지 않다")
    void noJdbcTimeZoneSetting() throws IOException {
        for (String module : MODULES) {
            Path yml = repositoryRoot().resolve(module).resolve("src/main/resources/application.yml");
            assertThat(yml).exists();
            assertThat(Files.readString(yml))
                    .as("%s/application.yml — JVM 시간대와 어긋나면 저장·조회에 ±9시간이 적용된다", module)
                    .doesNotContain("time_zone");
        }
    }

    @Test
    @DisplayName("세 모듈 이미지 모두 서비스 시간대(Asia/Seoul)로 고정돼 있다")
    void dockerImagesPinServiceTimeZone() throws IOException {
        for (String module : MODULES) {
            Path dockerfile = repositoryRoot().resolve(module).resolve("Dockerfile");
            assertThat(dockerfile).exists();
            assertThat(Files.readString(dockerfile))
                    .as("%s/Dockerfile — 빠지면 컨테이너가 UTC로 기동된다", module)
                    .contains("ENV TZ=Asia/Seoul");
        }
    }

    // 테스트 실행 디렉터리가 모듈마다 달라서, settings.gradle이 있는 곳까지 거슬러 올라간다.
    private Path repositoryRoot() {
        Path current = Paths.get("").toAbsolutePath();
        while (current != null && !Files.exists(current.resolve("settings.gradle"))) {
            current = current.getParent();
        }
        if (current == null) {
            throw new IllegalStateException("저장소 루트(settings.gradle)를 찾지 못했습니다.");
        }
        return current;
    }
}
