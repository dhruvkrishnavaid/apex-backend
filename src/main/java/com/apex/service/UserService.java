package com.apex.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.apex.dto.request.UserRequests.ChangePasswordRequest;
import com.apex.dto.request.UserRequests.ResetPasswordRequest;
import com.apex.dto.request.UserRequests.UpdateUserRequest;
import com.apex.dto.response.AuthResponses.UserResponse;
import com.apex.exception.BusinessRuleException;
import com.apex.exception.ResourceNotFoundException;
import com.apex.exception.UnauthorizedException;
import com.apex.mapper.UserMapper;
import com.apex.model.Role;
import com.apex.model.User;
import com.apex.repository.RefreshTokenRepository;
import com.apex.repository.RoleRepository;
import com.apex.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

  private final UserRepository userRepository;
  private final RoleRepository roleRepository;
  private final RefreshTokenRepository refreshTokenRepository;
  private final PasswordEncoder passwordEncoder;
  private final UserMapper userMapper;

  @Transactional(readOnly = true)
  public Page<UserResponse> listUsers(Pageable pageable) {
    return userRepository.findAllActiveWithRole(pageable).map(userMapper::toResponse);
  }

  @Transactional(readOnly = true)
  public UserResponse getUser(UUID id) {
    return userMapper.toResponse(find(id));
  }

  @Transactional
  public UserResponse updateUser(UUID id, UpdateUserRequest request, UUID actingUserId) {
    User user = find(id);
    Role role = roleRepository.findById(request.roleId())
        .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + request.roleId()));

    boolean deactivating = Boolean.FALSE.equals(request.isActive());
    if (id.equals(actingUserId) && (deactivating || !role.getId().equals(user.getRole().getId()))) {
      throw new BusinessRuleException("You cannot deactivate yourself or change your own role");
    }

    boolean roleChanged = !role.getId().equals(user.getRole().getId());
    user.setFirstName(request.firstName());
    user.setLastName(request.lastName());
    user.setRole(role);
    if (request.isActive() != null) {
      user.setIsActive(request.isActive());
    }
    user = userRepository.save(user);

    if (deactivating || roleChanged) {
      refreshTokenRepository.revokeAllByUserId(id);
    }
    return userMapper.toResponse(user);
  }

  @Transactional
  public void deleteUser(UUID id, UUID actingUserId) {
    if (id.equals(actingUserId)) {
      throw new BusinessRuleException("You cannot delete your own account");
    }
    User user = find(id);
    user.setDeletedAt(LocalDateTime.now());
    user.setIsActive(false);
    // Free the unique e-mail so it can be registered again.
    user.setEmail(user.getEmail() + ".deleted." + user.getId());
    userRepository.save(user);
    refreshTokenRepository.revokeAllByUserId(id);
  }

  @Transactional
  public void resetPassword(UUID id, ResetPasswordRequest request) {
    User user = find(id);
    user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
    userRepository.save(user);
    refreshTokenRepository.revokeAllByUserId(id);
  }

  @Transactional
  public void changePassword(UUID id, ChangePasswordRequest request) {
    User user = find(id);
    if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
      throw new UnauthorizedException("Current password is incorrect");
    }
    user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
    userRepository.save(user);
    refreshTokenRepository.revokeAllByUserId(id);
  }

  private User find(UUID id) {
    return userRepository.findByIdWithRole(id)
        .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
  }
}
