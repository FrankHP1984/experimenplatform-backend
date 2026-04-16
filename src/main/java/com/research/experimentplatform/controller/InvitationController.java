package com.research.experimentplatform.controller;

import com.research.experimentplatform.dto.AcceptInvitationRequest;
import com.research.experimentplatform.dto.CreateInvitationRequest;
import com.research.experimentplatform.dto.EnrollmentDTO;
import com.research.experimentplatform.dto.InvitationDTO;
import com.research.experimentplatform.service.InvitationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class InvitationController {

    private final InvitationService invitationService;

    public InvitationController(InvitationService invitationService) {
        this.invitationService = invitationService;
    }

    @PostMapping("/api/experiments/{experimentId}/invitations")
    @PreAuthorize("hasAnyRole('RESEARCHER', 'ADMIN')")
    public ResponseEntity<InvitationDTO> createInvitation(
            @PathVariable Long experimentId,
            @Valid @RequestBody CreateInvitationRequest request,
            Authentication authentication) {
        String supabaseId = (String) authentication.getDetails();
        InvitationDTO invitation = invitationService.createInvitation(experimentId, request, supabaseId);
        return ResponseEntity.status(HttpStatus.CREATED).body(invitation);
    }

    @GetMapping("/api/experiments/{experimentId}/invitations")
    @PreAuthorize("hasAnyRole('RESEARCHER', 'ADMIN')")
    public ResponseEntity<List<InvitationDTO>> getInvitationsByExperiment(
            @PathVariable Long experimentId,
            Authentication authentication) {
        String supabaseId = (String) authentication.getDetails();
        return ResponseEntity.ok(invitationService.getInvitationsByExperiment(experimentId, supabaseId));
    }

    @GetMapping("/api/invitations/{token}")
    public ResponseEntity<InvitationDTO> getInvitation(@PathVariable String token) {
        return ResponseEntity.ok(invitationService.getInvitationByToken(token));
    }

    @PostMapping("/api/invitations/{token}/accept")
    @PreAuthorize("hasAnyRole('PARTICIPANT', 'ADMIN')")
    public ResponseEntity<EnrollmentDTO> acceptInvitation(
            @PathVariable String token,
            @Valid @RequestBody AcceptInvitationRequest request,
            Authentication authentication) {
        String supabaseId = (String) authentication.getDetails();
        EnrollmentDTO enrollment = invitationService.acceptInvitation(token, request, supabaseId);
        return ResponseEntity.status(HttpStatus.CREATED).body(enrollment);
    }

    @PostMapping("/api/invitations/{token}/decline")
    @PreAuthorize("hasAnyRole('PARTICIPANT', 'ADMIN')")
    public ResponseEntity<Void> declineInvitation(
            @PathVariable String token,
            Authentication authentication) {
        String supabaseId = (String) authentication.getDetails();
        invitationService.declineInvitation(token, supabaseId);
        return ResponseEntity.noContent().build();
    }
}
