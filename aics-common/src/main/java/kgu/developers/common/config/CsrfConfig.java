package kgu.developers.common.config;

import java.time.Duration;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.DeferredCsrfToken;

/**
 * 인증을 쿠키로만 하므로 브라우저가 자동으로 붙이는 요청을 걸러낼 방어선이 필요하다.
 * 쿠키의 SameSite 속성 하나에만 기대지 않도록, 상태를 바꾸는 요청에 double-submit 토큰을 요구한다.
 * 프론트엔드는 XSRF-TOKEN 쿠키를 읽어 X-XSRF-TOKEN 헤더에 그대로 실어 보내면 된다.
 */
public final class CsrfConfig {
	private static final Duration COOKIE_MAX_AGE = Duration.ofDays(30);

	private CsrfConfig() {
	}

	public static Customizer<CsrfConfigurer<HttpSecurity>> spa(String... ignoredPaths) {
		return csrf -> {
			csrf.csrfTokenRepository(cookieTokenRepository())
				.csrfTokenRequestHandler(tokenRequestHandler());

			if (ignoredPaths.length > 0) {
				csrf.ignoringRequestMatchers(ignoredPaths);
			}
		};
	}

	private static CsrfTokenRepository cookieTokenRepository() {
		CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
		repository.setCookieCustomizer(cookie -> cookie.maxAge(COOKIE_MAX_AGE));
		return new SlidingCookieCsrfTokenRepository(repository);
	}

	private static CsrfTokenRequestAttributeHandler tokenRequestHandler() {
		CsrfTokenRequestAttributeHandler handler = new CsrfTokenRequestAttributeHandler();
		handler.setCsrfRequestAttributeName(null);
		return handler;
	}

	private record SlidingCookieCsrfTokenRepository(CsrfTokenRepository delegate)
		implements CsrfTokenRepository {

		@Override
		public DeferredCsrfToken loadDeferredToken(HttpServletRequest request, HttpServletResponse response) {
			CsrfToken loaded = delegate.loadToken(request);
			CsrfToken token = (loaded != null) ? loaded : delegate.generateToken(request);
			delegate.saveToken(token, request, response);

			return new DeferredCsrfToken() {
				@Override
				public CsrfToken get() {
					return token;
				}

				@Override
				public boolean isGenerated() {
					return loaded == null;
				}
			};
		}

		@Override
		public CsrfToken generateToken(HttpServletRequest request) {
			return delegate.generateToken(request);
		}

		@Override
		public void saveToken(CsrfToken token, HttpServletRequest request, HttpServletResponse response) {
			delegate.saveToken(token, request, response);
		}

		@Override
		public CsrfToken loadToken(HttpServletRequest request) {
			return delegate.loadToken(request);
		}
	}
}
