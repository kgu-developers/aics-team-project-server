package kgu.developers.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;

class CsrfConfigTest {

	@Test
	@DisplayName("공유 도메인이 없으면 XSRF-TOKEN을 host-only로 발급한다")
	void hostOnlyCookieWithoutConfiguredDomain() {
		MockHttpServletResponse response = issueCookie("");
		Cookie cookie = response.getCookie("XSRF-TOKEN");

		assertThat(cookie).isNotNull();
		assertThat(cookie.getDomain()).isNull();
		assertThat(response.getHeaders(HttpHeaders.SET_COOKIE)).hasSize(1);
	}

	@Test
	@DisplayName("공유 도메인이 있으면 XSRF-TOKEN에 Domain을 설정한다")
	void sharedCookieWithConfiguredDomain() {
		MockHttpServletResponse response = issueCookie("kgudevelopers.monster");
		Cookie cookie = response.getCookie("XSRF-TOKEN");

		assertThat(cookie).isNotNull();
		assertThat(cookie.getDomain()).isEqualTo("kgudevelopers.monster");
		assertThat(response.getHeaders(HttpHeaders.SET_COOKIE))
			.anySatisfy(header -> assertThat(header)
				.contains("XSRF-TOKEN=")
				.contains("Domain=kgudevelopers.monster"))
			.anySatisfy(header -> assertThat(header)
				.contains("XSRF-TOKEN=")
				.contains("Max-Age=0")
				.doesNotContain("Domain="));
	}

	private MockHttpServletResponse issueCookie(String domain) {
		CsrfTokenRepository repository = CsrfConfig.cookieTokenRepository(domain);
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		CsrfToken token = repository.generateToken(request);

		repository.saveToken(token, request, response);

		return response;
	}
}
