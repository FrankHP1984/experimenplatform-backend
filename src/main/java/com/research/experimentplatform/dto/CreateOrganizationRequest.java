package com.research.experimentplatform.dto;

import com.research.experimentplatform.model.OrganizationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateOrganizationRequest(

    @NotBlank(message = "Organization name is required")
    @Size(max = 255, message = "Name must not exceed 255 characters")
    String name,

    @Size(max = 500, message = "Description must not exceed 500 characters")
    String description,

    @Size(max = 255, message = "Website must not exceed 255 characters")
    String website,

    @NotNull(message = "Organization type is required")
    OrganizationType type
) {}
