package com.apex.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record CategoryResponse(
    UUID id,
    UUID parentId,
    String name,
    String slug,
    String description,
    Boolean isActive,
    List<CategoryResponse> subcategories,
    LocalDateTime createdAt) {
}
