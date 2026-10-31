package com.apex.config;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.apex.model.Role;
import com.apex.model.User;
import com.apex.repository.RoleRepository;
import com.apex.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

  private final RoleRepository roleRepository;
  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  @Value("${app.bootstrap.admin.email:}")
  private String adminEmail;

  @Value("${app.bootstrap.admin.password:}")
  private String adminPassword;

  @Override
  @Transactional
  public void run(String... args) {
    seedRole("ADMIN", List.of(
        "user:create", "user:read", "user:update", "user:delete",
        "role:read", "role:manage",
        "category:create", "category:read", "category:update", "category:delete",
        "product:create", "product:read", "product:update", "product:delete",
        "inventory:read", "inventory:adjust",
        "order:create", "order:read", "order:update", "order:cancel",
        "supplier:create", "supplier:read", "supplier:update", "supplier:delete",
        "warehouse:create", "warehouse:read", "warehouse:update", "warehouse:delete"));

    seedRole("MANAGER", List.of(
        "category:create", "category:read", "category:update",
        "product:create", "product:read", "product:update",
        "inventory:read", "inventory:adjust",
        "order:read", "order:update",
        "supplier:create", "supplier:read", "supplier:update",
        "warehouse:read"));

    seedRole("WAREHOUSE_STAFF", List.of(
        "category:read", "product:read",
        "inventory:read", "inventory:adjust",
        "order:read", "order:update",
        "warehouse:read"));

    seedRole("CASHIER", List.of(
        "category:read", "product:read",
        "inventory:read",
        "order:create", "order:read"));

    seedAdminUser();
  }

  private void seedAdminUser() {
    if (adminEmail == null || adminEmail.isBlank() || adminPassword == null || adminPassword.isBlank()) {
      return;
    }
    String email = adminEmail.trim().toLowerCase(Locale.ROOT);
    if (userRepository.existsByEmailAndDeletedAtIsNull(email)) {
      return;
    }
    Role admin = roleRepository.findByName("ADMIN").orElseThrow();
    userRepository.save(User.builder()
        .firstName("System")
        .lastName("Admin")
        .email(email)
        .passwordHash(passwordEncoder.encode(adminPassword))
        .role(admin)
        .build());
    log.info("Seeded bootstrap admin user '{}'", email);
  }

  private void seedRole(String name, List<String> permissions) {
    var existing = roleRepository.findByName(name);
    if (existing.isPresent()) {
      // Additive sync so permissions introduced by later sprints reach built-in roles.
      Role role = existing.get();
      Set<String> merged = new LinkedHashSet<>(role.getPermissions() != null ? role.getPermissions() : List.of());
      if (merged.addAll(permissions)) {
        role.setPermissions(new ArrayList<>(merged));
        roleRepository.save(role);
        log.info("Updated permissions of role '{}'", name);
      }
      return;
    }
    roleRepository.save(Role.builder().name(name).permissions(permissions).build());
    log.info("Seeded role '{}' with {} permissions", name, permissions.size());
  }
}
