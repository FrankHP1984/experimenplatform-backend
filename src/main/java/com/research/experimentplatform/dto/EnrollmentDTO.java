package com.research.experimentplatform.dto;

import com.research.experimentplatform.model.EnrollmentStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentDTO {

    private Long id;
    private Long participantId;
    private Long experimentId;
    private String experimentTitle;
    private Long groupId;
    private String groupName;
    private EnrollmentStatus status;
    private LocalDateTime enrolledAt;
    private LocalDateTime completedAt;
    private LocalDateTime consentSignedAt;
}
