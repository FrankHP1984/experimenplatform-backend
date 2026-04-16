package com.research.experimentplatform.dto;

import com.research.experimentplatform.model.UserRole;

public record UserDTO(
    Long id,
    String email,
    UserRole role
) {}
