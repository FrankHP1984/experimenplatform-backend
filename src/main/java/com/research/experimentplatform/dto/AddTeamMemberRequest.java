package com.research.experimentplatform.dto;

import com.research.experimentplatform.model.TeamRole;
import jakarta.validation.constraints.NotNull;

public record AddTeamMemberRequest(
    @NotNull(message = "User ID is required")
    Long userId,

    @NotNull(message = "Role is required")
    TeamRole role
) {}
