package com.research.experimentplatform.dto;

import com.research.experimentplatform.model.OrganizationType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationDTO {

    private Long id;
    private String name;
    private String description;
    private String website;
    private OrganizationType type;
    private LocalDateTime createdAt;
}
