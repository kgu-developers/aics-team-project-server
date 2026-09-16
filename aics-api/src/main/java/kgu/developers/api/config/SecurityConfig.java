package kgu.developers.api.config;

import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.DispatcherType;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import kgu.developers.common.config.CsrfConfig;
import kgu.developers.common.exception.JsonSecurityExceptionHandler;
import kgu.developers.globalutils.jwt.JwtCookieAuthenticationFilter;

@Configuration
public class SecurityConfig {

  private final JwtCookieAuthenticationFilter jwtCookieAuthenticationFilter;
  private final ObjectMapper objectMapper;
  private final String csrfCookieDomain;

  public SecurityConfig(JwtCookieAuthenticationFilter jwtCookieAuthenticationFilter,
      ObjectMapper objectMapper,
      @Value("${csrf.cookie-domain:}") String csrfCookieDomain) {
    this.jwtCookieAuthenticationFilter = jwtCookieAuthenticationFilter;
    this.objectMapper = objectMapper;
    this.csrfCookieDomain = csrfCookieDomain;
  }

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    return http
        .csrf(CsrfConfig.spa(csrfCookieDomain))
        .cors(Customizer.withDefaults())
        .sessionManagement(session -> session.sessionCreationPolicy(STATELESS))
        .authorizeHttpRequests(auth -> auth
            .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
            .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
            .anyRequest().authenticated())
        .exceptionHandling(e -> {
          JsonSecurityExceptionHandler handler = new JsonSecurityExceptionHandler(objectMapper);
          e.authenticationEntryPoint(handler).accessDeniedHandler(handler);
        })
        .addFilterBefore(jwtCookieAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
        .build();
  }
}
