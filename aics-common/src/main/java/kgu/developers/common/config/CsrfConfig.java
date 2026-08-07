package kgu.developers.common.config;

import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

/**
 * 인증을 쿠키로만 하므로 브라우저가 자동으로 붙이는 요청을 걸러낼 방어선이 필요하다.
 * 쿠키의 SameSite 속성 하나에만 기대지 않도록, 상태를 바꾸는 요청에 double-submit 토큰을 요구한다.
 * 프론트엔드는 XSRF-TOKEN 쿠키를 읽어 X-XSRF-TOKEN 헤더에 그대로 실어 보내면 된다.
 */
public final class CsrfConfig {

	private CsrfConfig() {
	}

	public static Customizer<CsrfConfigurer<HttpSecurity>> spa(String... ignoredPaths) {
		return csrf -> {
			csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
				.csrfTokenRequestHandler(tokenRequestHandler());

			if (ignoredPaths.length > 0) {
				csrf.ignoringRequestMatchers(ignoredPaths);
			}
		};
	}

	private static CsrfTokenRequestAttributeHandler tokenRequestHandler() {
		CsrfTokenRequestAttributeHandler handler = new CsrfTokenRequestAttributeHandler();
		handler.setCsrfRequestAttributeName(null);
		return handler;
	}
}
