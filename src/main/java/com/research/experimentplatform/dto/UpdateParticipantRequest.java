package com.research.experimentplatform.dto;

import jakarta.validation.constraints.Size;

public class UpdateParticipantRequest {

    @Size(max = 500, message = "Bio must not exceed 500 characters")
    private String bio;

    public UpdateParticipantRequest() {
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }
}
