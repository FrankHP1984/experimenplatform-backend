package com.research.experimentplatform.dto;

import com.research.experimentplatform.model.UserRole;

public record SyncUserRequest(UserRole role, String firstName, String lastName) {}
