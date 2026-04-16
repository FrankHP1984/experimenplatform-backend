package com.research.experimentplatform.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class EnrollParticipantRequest {

    @NotNull(message = "Experiment ID is required")
    private Long experimentId;

    private Long groupId;

    // Requerido cuando el experimento tiene texto de consentimiento
    private Boolean consentAgreed;
}
