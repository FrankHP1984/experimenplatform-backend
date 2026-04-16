package com.research.experimentplatform.dto;

import com.research.experimentplatform.model.InvitationStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InvitationDTO {

    private Long id;
    private Long experimentId;
    private String experimentTitle;
    private String consentText;
    private String invitedEmail;
    private Long groupId;
    private String groupName;
    private String token;
    private InvitationStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
}
