package com.apex.security;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/** Catalogue of every permission the API understands, and the built-in role bundles. */
public final class Permissions {

  private Permissions() {
  }

  private static List<String> crud(String resource) {
    return List.of(resource + ":create", resource + ":read", resource + ":update", resource + ":delete");
  }

  private static final List<String> ALL;

  static {
    List<String> all = new ArrayList<>();
    all.addAll(crud("user"));
    all.addAll(List.of("role:read", "role:manage"));
    all.addAll(crud("category"));
    all.addAll(crud("product"));
    all.addAll(crud("warehouse"));
    all.addAll(List.of("inventory:read", "inventory:adjust"));
    all.addAll(List.of("transfer:create", "transfer:read", "transfer:update"));
    all.addAll(crud("supplier"));
    all.addAll(List.of("purchase:create", "purchase:read", "purchase:update", "purchase:receive", "purchase:cancel"));
    all.addAll(crud("customer"));
    all.addAll(List.of("order:create", "order:read", "order:update", "order:cancel"));
    all.addAll(List.of("alert:read", "alert:manage"));
    all.addAll(List.of("report:read", "audit:read"));
    ALL = List.copyOf(all);
  }

  public static List<String> all() {
    return ALL;
  }

  public static boolean isValid(String permission) {
    return ALL.contains(permission);
  }

  /** Returns the permissions that are not part of the catalogue. */
  public static Set<String> unknown(Iterable<String> permissions) {
    Set<String> unknown = new TreeSet<>();
    for (String p : permissions) {
      if (!isValid(p)) {
        unknown.add(p);
      }
    }
    return unknown;
  }

  public static List<String> admin() {
    return ALL;
  }

  public static List<String> manager() {
    return List.of(
        "category:create", "category:read", "category:update",
        "product:create", "product:read", "product:update",
        "warehouse:read",
        "inventory:read", "inventory:adjust",
        "transfer:create", "transfer:read", "transfer:update",
        "supplier:create", "supplier:read", "supplier:update",
        "purchase:create", "purchase:read", "purchase:update", "purchase:receive", "purchase:cancel",
        "customer:create", "customer:read", "customer:update",
        "order:create", "order:read", "order:update", "order:cancel",
        "alert:read", "alert:manage",
        "report:read");
  }

  public static List<String> warehouseStaff() {
    return List.of(
        "category:read", "product:read",
        "warehouse:read",
        "inventory:read", "inventory:adjust",
        "transfer:create", "transfer:read", "transfer:update",
        "purchase:read", "purchase:receive",
        "order:read", "order:update",
        "alert:read");
  }

  public static List<String> cashier() {
    return List.of(
        "category:read", "product:read",
        "inventory:read",
        "customer:create", "customer:read",
        "order:create", "order:read");
  }
}
