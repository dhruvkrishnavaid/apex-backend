package com.apex.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.apex.dto.request.RoleRequests.CreateRoleRequest;
import com.apex.dto.request.RoleRequests.UpdateRoleRequest;
import com.apex.dto.response.RoleResponse;
import com.apex.service.RoleService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
public class RoleController {

  private final RoleService roleService;

  @GetMapping
  @PreAuthorize("hasAuthority('role:read')")
  public ResponseEntity<List<RoleResponse>> listRoles() {
    return ResponseEntity.ok(roleService.listRoles());
  }

  @GetMapping("/permissions")
  @PreAuthorize("hasAuthority('role:read')")
  public ResponseEntity<List<String>> listPermissions() {
    return ResponseEntity.ok(roleService.listPermissions());
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAuthority('role:read')")
  public ResponseEntity<RoleResponse> getRole(@PathVariable UUID id) {
    return ResponseEntity.ok(roleService.getRole(id));
  }

  @PostMapping
  @PreAuthorize("hasAuthority('role:manage')")
  public ResponseEntity<RoleResponse> createRole(@Valid @RequestBody CreateRoleRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(roleService.createRole(request));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAuthority('role:manage')")
  public ResponseEntity<RoleResponse> updateRole(
      @PathVariable UUID id,
      @Valid @RequestBody UpdateRoleRequest request) {
    return ResponseEntity.ok(roleService.updateRole(id, request));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasAuthority('role:manage')")
  public ResponseEntity<Void> deleteRole(@PathVariable UUID id) {
    roleService.deleteRole(id);
    return ResponseEntity.noContent().build();
  }
}
