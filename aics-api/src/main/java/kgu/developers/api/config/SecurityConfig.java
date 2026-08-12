package kgu.developers.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

  private final JwtCookieAuthenticationFilter jwtCookieAuthenticationFilter;

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    return http
        .csrf(AbstractHttpConfigurer::disable)
        .cors(Customizer.withDefaults())
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
            .anyRequest().authenticated())
        .addFilterBefore(jwtCookieAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
        .httpBasic(Customizer.withDefaults())
        .build();
  }
}
