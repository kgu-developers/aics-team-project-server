package kgu.developers.auth.config;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

import jakarta.servlet.DispatcherType;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import kgu.developers.common.config.CsrfConfig;
import kgu.developers.globalutils.jwt.JwtCookieAuthenticationFilter;
import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

  private final JwtCookieAuthenticationFilter jwtCookieAuthenticationFilter;

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    return http
        .csrf(CsrfConfig.spa("/api/v1/oop/auth/login"))
        .cors(Customizer.withDefaults())
        .sessionManagement(session -> session.sessionCreationPolicy(STATELESS))
        .authorizeHttpRequests(auth -> auth
            .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
            .requestMatchers("/api/v1/oop/auth/login", "/api/v1/oop/auth/refresh",
                "/api/v1/oop/auth/logout", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
            .anyRequest().authenticated())
        .exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(UNAUTHORIZED)))
        .addFilterBefore(jwtCookieAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
        .build();
  }
}
