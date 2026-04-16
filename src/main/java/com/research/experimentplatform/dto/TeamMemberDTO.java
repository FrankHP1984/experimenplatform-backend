package com.research.experimentplatform.dto;

import com.research.experimentplatform.model.TeamRole;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TeamMemberDTO {

    private Long userId;
    private String email;
    private TeamRole role;
    private LocalDateTime joinedAt;
}
