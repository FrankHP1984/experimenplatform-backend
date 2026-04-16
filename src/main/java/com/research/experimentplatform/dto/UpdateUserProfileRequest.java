package com.research.experimentplatform.dto;

import jakarta.validation.constraints.Size;

public record UpdateUserProfileRequest(
    String firstName,
    String lastName,
    String institution,
    String department,
    String position,
    
    @Size(max = 1000, message = "Bio must not exceed 1000 characters")
    String bio,
    
    String orcidId,
    String language,
    String timezone
) {}
