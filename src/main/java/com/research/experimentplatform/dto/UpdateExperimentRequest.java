package com.research.experimentplatform.dto;

import com.research.experimentplatform.model.DesignType;
import com.research.experimentplatform.model.ExperimentStatus;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record UpdateExperimentRequest(
    @Size(min = 1, max = 255, message = "Title must be between 1 and 255 characters")
    String title,

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    String description,

    DesignType designType,
    LocalDateTime startDate,
    LocalDateTime endDate,
    ExperimentStatus status,

    @Size(max = 5000, message = "Consent text must not exceed 5000 characters")
    String consentText,

    @Size(max = 5000, message = "Debriefing text must not exceed 5000 characters")
    String debriefingText
) {}
