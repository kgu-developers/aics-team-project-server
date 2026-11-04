package api.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class SwaggerUiConfigTest {

  @Test
  @DisplayName("Swagger UI는 쓰기 API 호출 시 CSRF 쿠키를 헤더로 전달한다")
  void swaggerUiCsrfSupportIsEnabled() {
    new ApplicationContextRunner()
        .withInitializer(new ConfigDataApplicationContextInitializer())
        .run(context -> assertThat(context.getEnvironment()
            .getProperty("springdoc.swagger-ui.csrf.enabled", Boolean.class))
            .isTrue());
  }

  @Test
  @DisplayName("OpenAPI 문서는 허용된 locale만 캐시한다")
  void openApiLocalesAreRestricted() {
    new ApplicationContextRunner()
        .withInitializer(new ConfigDataApplicationContextInitializer())
        .run(context -> assertThat(Binder.get(context.getEnvironment())
            .bind("springdoc.allowed-locales", Bindable.listOf(String.class))
            .orElse(List.of()))
            .containsExactly("ko-KR", "en-US", "en"));
  }
}
