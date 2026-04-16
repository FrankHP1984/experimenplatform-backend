package com.research.experimentplatform.controller;

import com.research.experimentplatform.dto.ParticipantDTO;
import com.research.experimentplatform.dto.UpdateParticipantRequest;
import com.research.experimentplatform.service.ParticipantService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/participants")
public class ParticipantController {

    private final ParticipantService participantService;

    public ParticipantController(ParticipantService participantService) {
        this.participantService = participantService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('PARTICIPANT', 'ADMIN')")
    public ResponseEntity<ParticipantDTO> createParticipant(Authentication authentication) {
        String supabaseId = (String) authentication.getDetails();
        ParticipantDTO participant = participantService.createParticipant(supabaseId);
        return ResponseEntity.status(HttpStatus.CREATED).body(participant);
    }

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('PARTICIPANT', 'ADMIN')")
    public ResponseEntity<ParticipantDTO> getMyProfile(Authentication authentication) {
        String supabaseId = (String) authentication.getDetails();
        ParticipantDTO participant = participantService.getParticipantByUserId(supabaseId);
        return ResponseEntity.ok(participant);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('RESEARCHER', 'ADMIN')")
    public ResponseEntity<ParticipantDTO> getParticipant(@PathVariable Long id) {
        ParticipantDTO participant = participantService.getParticipant(id);
        return ResponseEntity.ok(participant);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('PARTICIPANT', 'ADMIN')")
    public ResponseEntity<ParticipantDTO> updateParticipant(
            @PathVariable Long id,
            @Valid @RequestBody UpdateParticipantRequest request,
            Authentication authentication) {
        String supabaseId = (String) authentication.getDetails();
        ParticipantDTO participant = participantService.updateParticipant(id, request, supabaseId);
        return ResponseEntity.ok(participant);
    }
}
