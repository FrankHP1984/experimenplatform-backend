package com.research.experimentplatform.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreateInvitationRequest(
    @NotBlank(message = "Email is required")
    @Email(message = "Must be a valid email")
    String email,

    Long groupId
) {}
