package com.research.experimentplatform.dto;

import com.research.experimentplatform.model.DesignType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record CreateExperimentRequest(
    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title must not exceed 255 characters")
    String title,

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    String description,

    @NotNull(message = "Design type is required")
    DesignType designType,

    LocalDateTime startDate,
    LocalDateTime endDate,

    @Size(max = 5000, message = "Consent text must not exceed 5000 characters")
    String consentText,

    @Size(max = 5000, message = "Debriefing text must not exceed 5000 characters")
    String debriefingText
) {}
