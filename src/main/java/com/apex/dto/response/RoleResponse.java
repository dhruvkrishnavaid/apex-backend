package com.apex.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record RoleResponse(
    UUID id,
    String name,
    List<String> permissions,
    boolean builtIn,
    long userCount,
    LocalDateTime createdAt) {
}
