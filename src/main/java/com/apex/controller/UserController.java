package com.apex.controller;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.apex.dto.request.UserRequests.ChangePasswordRequest;
import com.apex.dto.request.UserRequests.ResetPasswordRequest;
import com.apex.dto.request.UserRequests.UpdateUserRequest;
import com.apex.dto.response.AuthResponses.UserResponse;
import com.apex.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class UserController {

  private final UserService userService;

  @GetMapping("/users")
  @PreAuthorize("hasAuthority('user:read')")
  public ResponseEntity<Page<UserResponse>> listUsers(@PageableDefault(size = 20) Pageable pageable) {
    return ResponseEntity.ok(userService.listUsers(pageable));
  }

  @GetMapping("/users/{id}")
  @PreAuthorize("hasAuthority('user:read')")
  public ResponseEntity<UserResponse> getUser(@PathVariable UUID id) {
    return ResponseEntity.ok(userService.getUser(id));
  }

  @PutMapping("/users/{id}")
  @PreAuthorize("hasAuthority('user:update')")
  public ResponseEntity<UserResponse> updateUser(
      @PathVariable UUID id,
      @Valid @RequestBody UpdateUserRequest request,
      Authentication authentication) {
    return ResponseEntity.ok(userService.updateUser(id, request, UUID.fromString(authentication.getName())));
  }

  @DeleteMapping("/users/{id}")
  @PreAuthorize("hasAuthority('user:delete')")
  public ResponseEntity<Void> deleteUser(@PathVariable UUID id, Authentication authentication) {
    userService.deleteUser(id, UUID.fromString(authentication.getName()));
    return ResponseEntity.noContent().build();
  }

  @PatchMapping("/users/{id}/password")
  @PreAuthorize("hasAuthority('user:update')")
  public ResponseEntity<Void> resetPassword(
      @PathVariable UUID id,
      @Valid @RequestBody ResetPasswordRequest request) {
    userService.resetPassword(id, request);
    return ResponseEntity.noContent().build();
  }

  @PatchMapping("/auth/password")
  public ResponseEntity<Void> changeOwnPassword(
      @Valid @RequestBody ChangePasswordRequest request,
      Authentication authentication) {
    userService.changePassword(UUID.fromString(authentication.getName()), request);
    return ResponseEntity.noContent().build();
  }
}
