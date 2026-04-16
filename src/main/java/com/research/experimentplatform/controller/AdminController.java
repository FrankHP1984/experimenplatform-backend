package com.research.experimentplatform.controller;

import com.research.experimentplatform.dto.UserDTO;
import com.research.experimentplatform.model.EnrollmentStatus;
import com.research.experimentplatform.model.ExperimentStatus;
import com.research.experimentplatform.model.UserRole;
import com.research.experimentplatform.repository.EnrollmentRepository;
import com.research.experimentplatform.repository.ExperimentRepository;
import com.research.experimentplatform.repository.ParticipantRepository;
import com.research.experimentplatform.repository.UserRepository;
import com.research.experimentplatform.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserService userService;
    private final UserRepository userRepository;
    private final ExperimentRepository experimentRepository;
    private final ParticipantRepository participantRepository;
    private final EnrollmentRepository enrollmentRepository;

    public AdminController(UserService userService,
                           UserRepository userRepository,
                           ExperimentRepository experimentRepository,
                           ParticipantRepository participantRepository,
                           EnrollmentRepository enrollmentRepository) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.experimentRepository = experimentRepository;
        this.participantRepository = participantRepository;
        this.enrollmentRepository = enrollmentRepository;
    }

    @GetMapping("/users")
    public ResponseEntity<Page<UserDTO>> getAllUsers(Pageable pageable) {
        return ResponseEntity.ok(userService.getAllUsers(pageable));
    }

    @PutMapping("/users/{id}/role")
    public ResponseEntity<UserDTO> updateUserRole(
            @PathVariable Long id,
            @RequestParam UserRole role) {
        return ResponseEntity.ok(userService.updateUserRole(id, role));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Long>> getStats() {
        return ResponseEntity.ok(Map.of(
                "totalUsers",       userRepository.count(),
                "totalExperiments", experimentRepository.count(),
                "activeExperiments",experimentRepository.countByStatus(ExperimentStatus.ACTIVE),
                "totalParticipants",participantRepository.count(),
                "totalEnrollments", enrollmentRepository.count(),
                "activeEnrollments",enrollmentRepository.countByStatus(EnrollmentStatus.ACTIVE)
        ));
    }
}
