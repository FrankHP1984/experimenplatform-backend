package com.research.experimentplatform.dto;

public class GroupDTO {

    private Long id;
    private String name;
    private String description;
    private String color;
    private Long experimentId;

    public GroupDTO() {
    }

    public GroupDTO(Long id, String name, String description, String color, Long experimentId) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.color = color;
        this.experimentId = experimentId;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public Long getExperimentId() { return experimentId; }
    public void setExperimentId(Long experimentId) { this.experimentId = experimentId; }
}
