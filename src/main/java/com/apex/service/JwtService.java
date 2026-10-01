package com.apex.service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collections;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.apex.model.User;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

@Service
public class JwtService {

  private final String secret;
  private final long ttlSeconds;
  private final String issuer;

  public JwtService(
      @Value("${app.jwt.secret}") String secret,
      @Value("${app.jwt.ttl-seconds:3600}") long ttlSeconds,
      @Value("${app.jwt.issuer:apex-backend}") String issuer) {
    this.secret = secret;
    this.ttlSeconds = ttlSeconds;
    this.issuer = issuer;
  }

  public long getTtlSeconds() {
    return ttlSeconds;
  }

  public String generateAccessToken(User user) {
    Instant now = Instant.now();

    JWTClaimsSet claims = new JWTClaimsSet.Builder()
        .issuer(issuer)
        .subject(user.getId().toString())
        .claim("email", user.getEmail())
        .claim("role", user.getRole() != null ? user.getRole().getName() : null)
        .claim("permissions",
            user.getRole() != null && user.getRole().getPermissions() != null
                ? user.getRole().getPermissions()
                : Collections.emptyList())
        .issueTime(java.util.Date.from(now))
        .expirationTime(java.util.Date.from(now.plusSeconds(ttlSeconds)))
        .build();

    try {
      JWSSigner signer = new MACSigner(secret.getBytes(StandardCharsets.UTF_8));
      SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
      jwt.sign(signer);
      return jwt.serialize();
    } catch (JOSEException e) {
      throw new IllegalStateException("Failed to sign JWT", e);
    }
  }
}
