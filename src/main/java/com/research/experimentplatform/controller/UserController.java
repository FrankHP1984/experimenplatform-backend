package com.research.experimentplatform.controller;

import com.research.experimentplatform.dto.SyncUserRequest;
import com.research.experimentplatform.dto.UpdateUserProfileRequest;
import com.research.experimentplatform.dto.UserDTO;
import com.research.experimentplatform.model.UserRole;
import com.research.experimentplatform.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserDTO> getMe(Authentication authentication) {
        String supabaseId = (String) authentication.getDetails();
        return ResponseEntity.ok(userService.getUserBySupabaseId(supabaseId));
    }

    @PutMapping("/me")
    public ResponseEntity<UserDTO> updateMyProfile(
            Authentication authentication,
            @Valid @RequestBody UpdateUserProfileRequest request) {
        String supabaseId = (String) authentication.getDetails();
        UserDTO updatedUser = userService.updateProfile(supabaseId, request);
        return ResponseEntity.ok(updatedUser);
    }

    @PostMapping("/sync")
    public ResponseEntity<UserDTO> syncUser(
            Authentication authentication,
            @RequestBody(required = false) SyncUserRequest request) {
        String supabaseId = (String) authentication.getDetails();
        String email = (String) authentication.getPrincipal();
        UserRole role = request != null ? request.role() : null;
        UserService.SyncResult result = userService.syncUser(supabaseId, email, role);
        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(result.user());
    }
}
