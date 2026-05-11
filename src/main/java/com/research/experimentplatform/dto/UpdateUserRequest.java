package com.research.experimentplatform.dto;

public record UpdateUserRequest(
    String name,
    String firstName,
    String lastName,
    String institution,
    String department,
    String position,
    String roleTitle,
    String bio,
    String orcidId,
    String language,
    String timezone,
    String researchArea,
    String preferredDesign
) {}
