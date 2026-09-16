package common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import kgu.developers.common.config.CorsConfig;

class CorsConfigTest {

	@Test
	@DisplayName("CORS 설정에 배포 도메인을 포함한 허용 오리진과 메서드가 정상 반영된다")
	void corsConfigurationSource() {
		CorsConfig corsConfig = new CorsConfig();
		List<String> origins = List.of(
			"http://localhost:5173",
			"http://localhost:3000",
			"https://team-project.kgudevelopers.monster"
		);
		CorsConfigurationSource source = corsConfig.corsConfigurationSource(origins);

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setRequestURI("/api/v1/auth/login");
		CorsConfiguration config = source.getCorsConfiguration(request);

		assertThat(config).isNotNull();
		assertThat(config.getAllowedOrigins()).containsExactlyElementsOf(origins);
		assertThat(config.getAllowedMethods()).contains("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
		assertThat(config.getAllowCredentials()).isTrue();
	}
}
