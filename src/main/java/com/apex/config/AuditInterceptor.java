package com.apex.config;

import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.apex.model.AuditLog;
import com.apex.repository.AuditLogRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/** Writes an audit entry for every successful POST/PUT/PATCH/DELETE made by an authenticated user. */
@Component
@RequiredArgsConstructor
public class AuditInterceptor implements HandlerInterceptor {

  private static final Logger log = LoggerFactory.getLogger(AuditInterceptor.class);
  private static final Set<String> MUTATING = Set.of("POST", "PUT", "PATCH", "DELETE");
  private static final String PREFIX = "/api/v1/";

  private final AuditLogRepository auditLogRepository;

  @Override
  public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
    try {
      if (!MUTATING.contains(request.getMethod()) || response.getStatus() >= 400
          || !request.getRequestURI().startsWith(PREFIX)) {
        return;
      }
      Authentication auth = SecurityContextHolder.getContext().getAuthentication();
      if (!(auth instanceof JwtAuthenticationToken jwtAuth)) {
        return;
      }
      Jwt jwt = jwtAuth.getToken();
      String path = request.getRequestURI();
      String rest = path.substring(PREFIX.length());
      int slash = rest.indexOf('/');
      auditLogRepository.save(AuditLog.builder()
          .userId(parse(jwt.getSubject()))
          .userEmail(jwt.getClaimAsString("email"))
          .method(request.getMethod())
          .path(path.length() > 500 ? path.substring(0, 500) : path)
          .resource(slash < 0 ? rest : rest.substring(0, slash))
          .statusCode(response.getStatus())
          .ipAddress(request.getRemoteAddr())
          .build());
    } catch (Exception e) {
      // auditing must never break the request that already succeeded
      log.error("Failed to write audit log", e);
    }
  }

  private UUID parse(String value) {
    try {
      return UUID.fromString(value);
    } catch (RuntimeException e) {
      return null;
    }
  }
}
