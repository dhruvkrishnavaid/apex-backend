package com.apex.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.apex.dto.request.AuthRequests.LoginRequest;
import com.apex.dto.request.AuthRequests.RefreshTokenRequest;
import com.apex.dto.request.AuthRequests.RegisterRequest;
import com.apex.dto.response.AuthResponses.AuthResponse;
import com.apex.dto.response.AuthResponses.UserResponse;
import com.apex.exception.DuplicateResourceException;
import com.apex.exception.ResourceNotFoundException;
import com.apex.exception.UnauthorizedException;
import com.apex.mapper.UserMapper;
import com.apex.model.RefreshToken;
import com.apex.model.Role;
import com.apex.model.User;
import com.apex.repository.RefreshTokenRepository;
import com.apex.repository.RoleRepository;
import com.apex.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

  private static final long REFRESH_TOKEN_TTL_DAYS = 7;

  private final UserRepository userRepository;
  private final RoleRepository roleRepository;
  private final RefreshTokenRepository refreshTokenRepository;
  private final UserMapper userMapper;
  private final JwtService jwtService;
  private final PasswordEncoder passwordEncoder;

  private final SecureRandom secureRandom = new SecureRandom();

  @Transactional
  public AuthResponse register(RegisterRequest request) {
    if (userRepository.existsByEmailAndDeletedAtIsNull(request.email())) {
      throw new DuplicateResourceException("A user with this email already exists");
    }

    Role role = roleRepository.findById(request.roleId())
        .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + request.roleId()));

    User user = User.builder()
        .firstName(request.firstName())
        .lastName(request.lastName())
        .email(request.email())
        .passwordHash(passwordEncoder.encode(request.password()))
        .role(role)
        .build();

    user = userRepository.save(user);
    return issueTokens(user);
  }

  @Transactional
  public AuthResponse login(LoginRequest request) {
    User user = userRepository.findByEmailAndDeletedAtIsNull(request.email())
        .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

    if (!Boolean.TRUE.equals(user.getIsActive())) {
      throw new UnauthorizedException("Account is deactivated");
    }

    if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
      throw new UnauthorizedException("Invalid email or password");
    }

    return issueTokens(user);
  }

  @Transactional
  public AuthResponse refresh(RefreshTokenRequest request) {
    RefreshToken stored = refreshTokenRepository
        .findByTokenHashAndRevokedFalse(sha256(request.refreshToken()))
        .orElseThrow(() -> new UnauthorizedException("Invalid or revoked refresh token"));

    if (stored.getExpiresAt().isBefore(LocalDateTime.now())) {
      throw new UnauthorizedException("Refresh token has expired");
    }

    // Rotate the refresh token
    stored.setRevoked(true);
    refreshTokenRepository.save(stored);

    User user = userRepository.findByIdWithRole(stored.getUser().getId())
        .orElseThrow(() -> new UnauthorizedException("User no longer exists"));

    return issueTokens(user);
  }

  @Transactional
  public void logout(RefreshTokenRequest request) {
    refreshTokenRepository.findByTokenHashAndRevokedFalse(sha256(request.refreshToken()))
        .ifPresent(token -> {
          token.setRevoked(true);
          refreshTokenRepository.save(token);
        });
  }

  public UserResponse getUserById(UUID id) {
    User user = userRepository.findByIdWithRole(id)
        .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
    return userMapper.toResponse(user);
  }

  private AuthResponse issueTokens(User user) {
    String accessToken = jwtService.generateAccessToken(user);
    String refreshToken = generateOpaqueToken();

    RefreshToken token = RefreshToken.builder()
        .user(user)
        .tokenHash(sha256(refreshToken))
        .expiresAt(LocalDateTime.now().plusDays(REFRESH_TOKEN_TTL_DAYS))
        .build();
    refreshTokenRepository.save(token);

    return new AuthResponse(accessToken, refreshToken, jwtService.getTtlSeconds(),
        userMapper.toResponse(user));
  }

  private String generateOpaqueToken() {
    byte[] bytes = new byte[32];
    secureRandom.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  private String sha256(String value) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 not available", e);
    }
  }
}
