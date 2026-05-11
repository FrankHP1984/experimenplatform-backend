package com.research.experimentplatform.dto;

import com.research.experimentplatform.model.UserRole;

public record UserDTO(
    Long id,
    String email,
    UserRole role,
    String firstName,
    String lastName,
    String institution,
    String department,
    String position,
    String bio,
    String orcidId,
    String language,
    String timezone,
    String researchArea,
    String preferredDesign
) {}
