package com.apex.security;

import java.util.Optional;
import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class CurrentUser {

  private CurrentUser() {
  }

  /** Id of the authenticated user (the JWT subject), if the request is authenticated. */
  public static Optional<UUID> id() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated()) {
      return Optional.empty();
    }
    try {
      return Optional.of(UUID.fromString(auth.getName()));
    } catch (IllegalArgumentException e) {
      return Optional.empty();
    }
  }
}
