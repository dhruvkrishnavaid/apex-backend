package com.apex.config;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;

import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

  private static final int MIN_SECRET_BYTES = 32;

  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .csrf(csrf -> csrf.disable())
        .cors(Customizer.withDefaults())
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/api/v1/auth/login", "/api/v1/auth/refresh", "/api/v1/auth/logout").permitAll()
            .requestMatchers("/api/v1/health").permitAll()
            .anyRequest().authenticated())
        .exceptionHandling(ex -> ex
            .authenticationEntryPoint(authenticationEntryPoint())
            .accessDeniedHandler(accessDeniedHandler()))
        .oauth2ResourceServer(oauth2 -> oauth2
            .authenticationEntryPoint(authenticationEntryPoint())
            .accessDeniedHandler(accessDeniedHandler())
            .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));

    return http.build();
  }

  @Bean
  JwtDecoder jwtDecoder(
      @Value("${app.jwt.secret}") String jwtSecret,
      @Value("${app.jwt.issuer:apex-backend}") String issuer) {
    byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
    if (keyBytes.length < MIN_SECRET_BYTES) {
      throw new IllegalStateException("app.jwt.secret must be at least " + MIN_SECRET_BYTES + " characters long");
    }
    SecretKey key = new SecretKeySpec(keyBytes, "HmacSHA256");
    NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key).build();
    decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
        JwtValidators.createDefaultWithIssuer(issuer)));
    return decoder;
  }

  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  /**
   * Extracts authorities from the JWT "permissions" claim so they can be used
   * with @PreAuthorize("hasAuthority('...')").
   */
  @Bean
  Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter() {
    return jwt -> {
      List<String> permissions = jwt.getClaimAsStringList("permissions");
      Collection<GrantedAuthority> authorities = permissions == null
          ? List.of()
          : permissions.stream().map(p -> (GrantedAuthority) new SimpleGrantedAuthority(p)).toList();
      return new JwtAuthenticationToken(jwt, authorities, jwt.getSubject());
    };
  }

  private AuthenticationEntryPoint authenticationEntryPoint() {
    return (request, response, ex) -> writeError(response, HttpStatus.UNAUTHORIZED,
        "Authentication required: missing or invalid access token");
  }

  private AccessDeniedHandler accessDeniedHandler() {
    return (request, response, ex) -> writeError(response, HttpStatus.FORBIDDEN,
        "Access denied: insufficient permissions");
  }

  private void writeError(HttpServletResponse response, HttpStatus status, String message) throws java.io.IOException {
    response.setStatus(status.value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    String body = "{\"status\":" + status.value()
        + ",\"message\":\"" + message.replace("\"", "'")
        + "\",\"details\":null,\"timestamp\":\"" + LocalDateTime.now() + "\"}";
    response.getWriter().write(body);
  }
}
