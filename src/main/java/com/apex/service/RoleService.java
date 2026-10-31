package com.apex.service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.apex.dto.request.RoleRequests.CreateRoleRequest;
import com.apex.dto.request.RoleRequests.UpdateRoleRequest;
import com.apex.dto.response.RoleResponse;
import com.apex.exception.BusinessRuleException;
import com.apex.exception.DuplicateResourceException;
import com.apex.exception.ResourceNotFoundException;
import com.apex.model.Role;
import com.apex.repository.RoleRepository;
import com.apex.repository.UserRepository;
import com.apex.security.Permissions;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RoleService {

  public static final Set<String> BUILT_IN = Set.of("ADMIN", "MANAGER", "WAREHOUSE_STAFF", "CASHIER");

  private final RoleRepository roleRepository;
  private final UserRepository userRepository;

  @Transactional(readOnly = true)
  public List<RoleResponse> listRoles() {
    return roleRepository.findAll().stream().map(this::toResponse).toList();
  }

  @Transactional(readOnly = true)
  public RoleResponse getRole(UUID id) {
    return toResponse(find(id));
  }

  public List<String> listPermissions() {
    return Permissions.all();
  }

  @Transactional
  public RoleResponse createRole(CreateRoleRequest request) {
    String name = normalizeName(request.name());
    if (roleRepository.existsByName(name)) {
      throw new DuplicateResourceException("Role already exists: " + name);
    }
    List<String> permissions = validate(request.permissions());
    return toResponse(roleRepository.save(Role.builder().name(name).permissions(permissions).build()));
  }

  @Transactional
  public RoleResponse updateRole(UUID id, UpdateRoleRequest request) {
    Role role = find(id);
    String name = normalizeName(request.name());
    List<String> permissions = validate(request.permissions());

    if (BUILT_IN.contains(role.getName()) && !role.getName().equals(name)) {
      throw new BusinessRuleException("Built-in roles cannot be renamed");
    }
    if ("ADMIN".equals(role.getName()) && !permissions.containsAll(Permissions.admin())) {
      throw new BusinessRuleException("The ADMIN role must keep every permission");
    }
    roleRepository.findByName(name).filter(other -> !other.getId().equals(id)).ifPresent(other -> {
      throw new DuplicateResourceException("Role already exists: " + name);
    });

    role.setName(name);
    role.setPermissions(permissions);
    return toResponse(roleRepository.save(role));
  }

  @Transactional
  public void deleteRole(UUID id) {
    Role role = find(id);
    if (BUILT_IN.contains(role.getName())) {
      throw new BusinessRuleException("Built-in roles cannot be deleted");
    }
    if (userRepository.countByRoleIdAndDeletedAtIsNull(id) > 0) {
      throw new IllegalStateException("Role is still assigned to users. Reassign them first.");
    }
    roleRepository.delete(role);
  }

  private Role find(UUID id) {
    return roleRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + id));
  }

  private String normalizeName(String name) {
    return name.trim().toUpperCase().replaceAll("\\s+", "_");
  }

  private List<String> validate(List<String> permissions) {
    Set<String> unknown = Permissions.unknown(permissions);
    if (!unknown.isEmpty()) {
      throw new IllegalArgumentException("Unknown permissions: " + String.join(", ", unknown));
    }
    return new ArrayList<>(new LinkedHashSet<>(permissions));
  }

  private RoleResponse toResponse(Role role) {
    return new RoleResponse(role.getId(), role.getName(), role.getPermissions(),
        BUILT_IN.contains(role.getName()),
        userRepository.countByRoleIdAndDeletedAtIsNull(role.getId()), role.getCreatedAt());
  }
}
