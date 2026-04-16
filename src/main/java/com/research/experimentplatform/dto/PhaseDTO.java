package com.research.experimentplatform.dto;

import java.time.LocalDateTime;

public class PhaseDTO {

    private Long id;
    private String name;
    private Integer phaseOrder;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Long experimentId;

    public PhaseDTO() {
    }

    public PhaseDTO(Long id, String name, Integer phaseOrder, LocalDateTime startDate, 
                    LocalDateTime endDate, Long experimentId) {
        this.id = id;
        this.name = name;
        this.phaseOrder = phaseOrder;
        this.startDate = startDate;
        this.endDate = endDate;
        this.experimentId = experimentId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getPhaseOrder() {
        return phaseOrder;
    }

    public void setPhaseOrder(Integer phaseOrder) {
        this.phaseOrder = phaseOrder;
    }

    public LocalDateTime getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
    }

    public LocalDateTime getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }

    public Long getExperimentId() {
        return experimentId;
    }

    public void setExperimentId(Long experimentId) {
        this.experimentId = experimentId;
    }
}
