package kgu.developers.globalutils.jwt;

import static kgu.developers.globalutils.jwt.JwtUtil.ISSUED_AT_MILLIS;
import static kgu.developers.globalutils.jwt.JwtUtil.ROLE;
import static kgu.developers.globalutils.jwt.JwtUtil.PASSWORD_CHANGE_REQUIRED;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.DataAccessException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.WebUtils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import kgu.developers.common.exception.JsonSecurityExceptionHandler;

@Component
@RequiredArgsConstructor
public class JwtCookieAuthenticationFilter extends OncePerRequestFilter {

	private static final String ACCESS_TOKEN = "accessToken";

	private final JwtUtil jwtUtil;
	private final TokenRevocationStore revocationStore;
	private final ObjectProvider<PasswordChangeRequirementChecker> passwordChangeRequirementChecker;
	private final ObjectMapper objectMapper;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
		FilterChain filterChain) throws ServletException, IOException {
		String token = accessToken(request);

		if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
			try {
				Claims claims = jwtUtil.parseAccessTokenClaims(token);
				if (revocationStore.isRevoked(claims.getSubject(), claims.get(ISSUED_AT_MILLIS, Long.class))) {
					SecurityContextHolder.clearContext();
				} else {
					PasswordChangeRequirementChecker checker = passwordChangeRequirementChecker.getIfAvailable();
					if ((Boolean.TRUE.equals(claims.get(PASSWORD_CHANGE_REQUIRED, Boolean.class))
						|| (checker != null && checker.isRequired(claims.getSubject())))
						&& !isPasswordChangeRequest(request, claims.getSubject())) {
						new JsonSecurityExceptionHandler(objectMapper).handle(request, response,
							new AccessDeniedException("비밀번호 변경이 필요합니다."));
						return;
					}
					SecurityContextHolder.getContext().setAuthentication(authentication(claims));
				}
			} catch (JwtException e) {
				SecurityContextHolder.clearContext();
			} catch (DataAccessException e) {
				logger.warn("무효화 목록을 조회할 수 없어 인증을 거절합니다.", e);
				SecurityContextHolder.clearContext();
			}
		}

		filterChain.doFilter(request, response);
	}

	private boolean isPasswordChangeRequest(HttpServletRequest request, String studentNumber) {
		if (!"PUT".equals(request.getMethod())) {
			return false;
		}
		String uri = request.getRequestURI();
		return ("/api/v1/oop/users/" + studentNumber + "/password").equals(uri)
			|| ("/api/v1/users/" + studentNumber + "/password").equals(uri);
	}

	private Authentication authentication(Claims claims) {
		String role = claims.get(ROLE, String.class);
		List<GrantedAuthority> authorities = role == null
			? List.of()
			: List.of(new SimpleGrantedAuthority("ROLE_" + role));

		return new UsernamePasswordAuthenticationToken(claims.getSubject(), null, authorities);
	}

	private String accessToken(HttpServletRequest request) {
		Cookie cookie = WebUtils.getCookie(request, ACCESS_TOKEN);
		return cookie == null ? null : cookie.getValue();
	}
}
