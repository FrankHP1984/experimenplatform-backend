package com.research.experimentplatform.controller;

import com.research.experimentplatform.dto.AddTeamMemberRequest;
import com.research.experimentplatform.dto.CreateTeamRequest;
import com.research.experimentplatform.dto.TeamDTO;
import com.research.experimentplatform.dto.TeamMemberDTO;
import com.research.experimentplatform.service.TeamService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/teams")
@PreAuthorize("hasAnyRole('RESEARCHER', 'ADMIN')")
public class TeamController {

    private final TeamService teamService;

    public TeamController(TeamService teamService) {
        this.teamService = teamService;
    }

    @PostMapping
    public ResponseEntity<TeamDTO> createTeam(
            @Valid @RequestBody CreateTeamRequest request,
            Authentication authentication) {
        String supabaseId = (String) authentication.getDetails();
        return ResponseEntity.status(HttpStatus.CREATED).body(teamService.createTeam(request, supabaseId));
    }

    @GetMapping("/my")
    public ResponseEntity<List<TeamDTO>> getMyTeams(Authentication authentication) {
        String supabaseId = (String) authentication.getDetails();
        return ResponseEntity.ok(teamService.getMyTeams(supabaseId));
    }

    @GetMapping("/{teamId}")
    public ResponseEntity<TeamDTO> getTeam(
            @PathVariable Long teamId,
            Authentication authentication) {
        String supabaseId = (String) authentication.getDetails();
        return ResponseEntity.ok(teamService.getTeam(teamId, supabaseId));
    }

    @GetMapping("/{teamId}/members")
    public ResponseEntity<List<TeamMemberDTO>> getTeamMembers(
            @PathVariable Long teamId,
            Authentication authentication) {
        String supabaseId = (String) authentication.getDetails();
        return ResponseEntity.ok(teamService.getTeamMembers(teamId, supabaseId));
    }

    @PostMapping("/{teamId}/members")
    public ResponseEntity<TeamMemberDTO> addMember(
            @PathVariable Long teamId,
            @Valid @RequestBody AddTeamMemberRequest request,
            Authentication authentication) {
        String supabaseId = (String) authentication.getDetails();
        return ResponseEntity.status(HttpStatus.CREATED).body(teamService.addMember(teamId, request, supabaseId));
    }

    @DeleteMapping("/{teamId}/members/{userId}")
    public ResponseEntity<Void> removeMember(
            @PathVariable Long teamId,
            @PathVariable Long userId,
            Authentication authentication) {
        String supabaseId = (String) authentication.getDetails();
        teamService.removeMember(teamId, userId, supabaseId);
        return ResponseEntity.noContent().build();
    }
}
