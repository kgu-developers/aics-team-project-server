package auth.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class SwaggerLocaleConfigTest {

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
