package com.research.experimentplatform.dto;

import java.time.LocalDateTime;

public class ParticipantDTO {

    private Long id;
    private Long userId;
    private String email;
    private String bio;
    private LocalDateTime createdAt;

    public ParticipantDTO() {
    }

    public ParticipantDTO(Long id, Long userId, String email, String bio, LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.email = email;
        this.bio = bio;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
