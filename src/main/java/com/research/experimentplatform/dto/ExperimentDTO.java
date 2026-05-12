package com.research.experimentplatform.dto;

import com.research.experimentplatform.model.DesignType;
import com.research.experimentplatform.model.ExperimentStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ExperimentDTO {

    private Long id;
    private String title;
    private String description;
    private DesignType designType;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private ExperimentStatus status;
    private Long ownerId;
    private String consentText;
    private String debriefingText;
    private Long organizationId;
    private String organizationName;
    private Long teamId;
    private String teamName;
    private boolean allowLateEnrollment;
}
