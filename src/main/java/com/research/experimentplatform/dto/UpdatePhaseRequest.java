package com.research.experimentplatform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public class UpdatePhaseRequest {

    @NotBlank(message = "Phase name is required")
    @Size(max = 100, message = "Phase name must not exceed 100 characters")
    private String name;

    private Integer phaseOrder;

    private LocalDateTime startDate;

    private LocalDateTime endDate;

    private Long groupId;

    private boolean clearGroup;

    public UpdatePhaseRequest() {
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Integer getPhaseOrder() { return phaseOrder; }
    public void setPhaseOrder(Integer phaseOrder) { this.phaseOrder = phaseOrder; }

    public LocalDateTime getStartDate() { return startDate; }
    public void setStartDate(LocalDateTime startDate) { this.startDate = startDate; }

    public LocalDateTime getEndDate() { return endDate; }
    public void setEndDate(LocalDateTime endDate) { this.endDate = endDate; }

    public Long getGroupId() { return groupId; }
    public void setGroupId(Long groupId) { this.groupId = groupId; }

    public boolean isClearGroup() { return clearGroup; }
    public void setClearGroup(boolean clearGroup) { this.clearGroup = clearGroup; }
}
