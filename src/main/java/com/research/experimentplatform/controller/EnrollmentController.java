package com.research.experimentplatform.controller;

import com.research.experimentplatform.dto.EnrollParticipantRequest;
import com.research.experimentplatform.dto.EnrollmentDTO;
import com.research.experimentplatform.model.EnrollmentStatus;
import com.research.experimentplatform.service.EnrollmentService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/enrollments")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    public EnrollmentController(EnrollmentService enrollmentService) {
        this.enrollmentService = enrollmentService;
    }

    @GetMapping("/{enrollmentId}")
    public ResponseEntity<EnrollmentDTO> getEnrollment(@PathVariable Long enrollmentId) {
        return ResponseEntity.ok(enrollmentService.getEnrollmentById(enrollmentId));
    }

    @PostMapping("/participants/{participantId}")
    @PreAuthorize("hasAnyRole('PARTICIPANT', 'ADMIN')")
    public ResponseEntity<EnrollmentDTO> enrollInExperiment(
            @PathVariable Long participantId,
            @Valid @RequestBody EnrollParticipantRequest request) {
        EnrollmentDTO enrollment = enrollmentService.enrollParticipant(participantId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(enrollment);
    }

    @GetMapping("/participants/{participantId}")
    @PreAuthorize("hasAnyRole('RESEARCHER', 'ADMIN')")
    public ResponseEntity<Page<EnrollmentDTO>> getParticipantEnrollments(
            @PathVariable Long participantId, Pageable pageable) {
        Page<EnrollmentDTO> enrollments = enrollmentService.getParticipantEnrollments(participantId, pageable);
        return ResponseEntity.ok(enrollments);
    }

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('PARTICIPANT', 'ADMIN')")
    public ResponseEntity<Page<EnrollmentDTO>> getMyEnrollments(
            Authentication authentication, Pageable pageable) {
        String supabaseId = (String) authentication.getDetails();
        Page<EnrollmentDTO> enrollments = enrollmentService.getEnrollmentsByUserId(supabaseId, pageable);
        return ResponseEntity.ok(enrollments);
    }

    @GetMapping("/experiments/{experimentId}")
    @PreAuthorize("hasAnyRole('RESEARCHER', 'ADMIN')")
    public ResponseEntity<Page<EnrollmentDTO>> getExperimentEnrollments(
            @PathVariable Long experimentId, Pageable pageable) {
        Page<EnrollmentDTO> enrollments = enrollmentService.getExperimentEnrollments(experimentId, pageable);
        return ResponseEntity.ok(enrollments);
    }

    @PutMapping("/{enrollmentId}/status")
    @PreAuthorize("hasAnyRole('RESEARCHER', 'ADMIN')")
    public ResponseEntity<EnrollmentDTO> updateEnrollmentStatus(
            @PathVariable Long enrollmentId,
            @RequestParam EnrollmentStatus status) {
        EnrollmentDTO enrollment = enrollmentService.updateEnrollmentStatus(enrollmentId, status);
        return ResponseEntity.ok(enrollment);
    }

    @PatchMapping("/{enrollmentId}/group")
    @PreAuthorize("hasAnyRole('RESEARCHER', 'ADMIN')")
    public ResponseEntity<EnrollmentDTO> assignGroup(
            @PathVariable Long enrollmentId,
            @RequestParam(required = false) Long groupId) {
        EnrollmentDTO enrollment = enrollmentService.assignGroup(enrollmentId, groupId);
        return ResponseEntity.ok(enrollment);
    }

    @PutMapping("/{enrollmentId}/complete")
    @PreAuthorize("hasAnyRole('PARTICIPANT', 'ADMIN')")
    public ResponseEntity<EnrollmentDTO> completeEnrollment(
            @PathVariable Long enrollmentId,
            Authentication authentication) {
        String supabaseId = (String) authentication.getDetails();
        EnrollmentDTO enrollment = enrollmentService.completeEnrollment(enrollmentId, supabaseId);
        return ResponseEntity.ok(enrollment);
    }

    @PatchMapping("/{enrollmentId}/consent")
    @PreAuthorize("hasAnyRole('PARTICIPANT', 'ADMIN')")
    public ResponseEntity<EnrollmentDTO> signConsent(@PathVariable Long enrollmentId) {
        EnrollmentDTO enrollment = enrollmentService.signConsent(enrollmentId);
        return ResponseEntity.ok(enrollment);
    }

    @DeleteMapping("/{enrollmentId}")
    @PreAuthorize("hasAnyRole('PARTICIPANT', 'ADMIN')")
    public ResponseEntity<Void> withdrawEnrollment(@PathVariable Long enrollmentId) {
        enrollmentService.withdrawEnrollment(enrollmentId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{enrollmentId}/data")
    @PreAuthorize("hasAnyRole('PARTICIPANT', 'ADMIN', 'RESEARCHER')")
    public ResponseEntity<Void> deleteParticipantData(@PathVariable Long enrollmentId) {
        enrollmentService.deleteParticipantData(enrollmentId);
        return ResponseEntity.noContent().build();
    }
}
