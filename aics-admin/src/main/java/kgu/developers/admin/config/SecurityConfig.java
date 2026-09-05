package kgu.developers.admin.config;

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
        .csrf(CsrfConfig.spa())
        .cors(Customizer.withDefaults())
        .sessionManagement(session -> session.sessionCreationPolicy(STATELESS))
        .authorizeHttpRequests(auth -> auth
            .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
            .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/admin-docs/**", "/admin-api-docs/**").permitAll()
            // 명단 업로드는 조교도 쓰므로 ROLE_ADMIN을 요구하지 않는다.
            // 대신 SectionStaffValidator가 담당 분반인지 확인한다 — 이 규칙이 인증만 확인하고
            // 인가는 안 하므로, 이 경로들 밑에 새 엔드포인트를 추가하면 컨트롤러/파사드에서
            // SectionStaffValidator를 직접 호출하는 걸 잊지 않도록 각별히 주의할 것.
            .requestMatchers("/api/v1/admin/sections/*/enrollment-imports/**",
                "/api/v1/admin/sections/*/team-imports/**",
                "/api/v1/admin/enrollment-imports/**",
                "/api/v1/admin/team-imports/**").authenticated()
            .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
            .anyRequest().authenticated())
        .exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(UNAUTHORIZED)))
        .addFilterBefore(jwtCookieAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
        .build();
  }
}
