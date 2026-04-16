package com.research.experimentplatform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTeamRequest(
    @NotBlank(message = "Team name is required")
    @Size(max = 255)
    String name,

    @Size(max = 500)
    String description
) {}
