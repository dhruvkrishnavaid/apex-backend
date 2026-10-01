package com.apex.config;

import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.apex.model.Role;
import com.apex.repository.RoleRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

  private final RoleRepository roleRepository;

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
        "product:read",
        "inventory:read", "inventory:adjust",
        "order:read", "order:update",
        "warehouse:read"));

    seedRole("CASHIER", List.of(
        "product:read",
        "inventory:read",
        "order:create", "order:read"));
  }

  private void seedRole(String name, List<String> permissions) {
    if (roleRepository.existsByName(name)) {
      log.debug("Role '{}' already exists, skipping", name);
      return;
    }
    roleRepository.save(Role.builder().name(name).permissions(permissions).build());
    log.info("Seeded role '{}' with {} permissions", name, permissions.size());
  }
}
