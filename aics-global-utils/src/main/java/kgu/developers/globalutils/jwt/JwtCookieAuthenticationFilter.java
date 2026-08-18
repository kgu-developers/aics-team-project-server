package kgu.developers.globalutils.jwt;

import static kgu.developers.globalutils.jwt.JwtUtil.ISSUED_AT_MILLIS;
import static kgu.developers.globalutils.jwt.JwtUtil.ROLE;

import java.io.IOException;
import java.util.List;

import org.springframework.dao.DataAccessException;
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

@Component
@RequiredArgsConstructor
public class JwtCookieAuthenticationFilter extends OncePerRequestFilter {

	private static final String ACCESS_TOKEN = "accessToken";

	private final JwtUtil jwtUtil;
	private final TokenRevocationStore revocationStore;

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
