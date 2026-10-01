package com.apex.mapper;

import com.apex.dto.response.AuthResponses.UserResponse;
import com.apex.model.User;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component
public class UserMapper {

  public UserResponse toResponse(User user) {
    if (user == null)
      return null;

    return new UserResponse(
        user.getId(),
        user.getFirstName(),
        user.getLastName(),
        user.getEmail(),
        user.getRole() != null ? user.getRole().getName() : null,
        user.getRole() != null && user.getRole().getPermissions() != null
            ? user.getRole().getPermissions()
            : Collections.emptyList(),
        user.getIsActive(),
        user.getMfaEnabled(),
        user.getCreatedAt());
  }
}
