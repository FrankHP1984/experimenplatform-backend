package com.research.experimentplatform.controller;

import com.research.experimentplatform.dto.CreateExperimentRequest;
import com.research.experimentplatform.dto.CreateGroupRequest;
import com.research.experimentplatform.dto.CreatePhaseRequest;
import com.research.experimentplatform.dto.UpdatePhaseRequest;
import com.research.experimentplatform.dto.ExperimentDTO;
import com.research.experimentplatform.dto.GroupDTO;
import com.research.experimentplatform.dto.PhaseDTO;
import com.research.experimentplatform.dto.UpdateExperimentRequest;
import com.research.experimentplatform.dto.EnrollmentDTO;
import com.research.experimentplatform.dto.ResponseDTO;
import com.research.experimentplatform.service.EnrollmentService;
import com.research.experimentplatform.service.ExperimentService;
import com.research.experimentplatform.service.GroupService;
import com.research.experimentplatform.service.PhaseService;
import com.research.experimentplatform.service.ResponseService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.research.experimentplatform.model.ExperimentStatus;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/experiments")
public class ExperimentController {

    private final ExperimentService experimentService;
    private final GroupService groupService;
    private final PhaseService phaseService;
    private final EnrollmentService enrollmentService;
    private final ResponseService responseService;

    public ExperimentController(ExperimentService experimentService, GroupService groupService,
                                PhaseService phaseService, EnrollmentService enrollmentService,
                                ResponseService responseService) {
        this.experimentService = experimentService;
        this.groupService = groupService;
        this.phaseService = phaseService;
        this.enrollmentService = enrollmentService;
        this.responseService = responseService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('RESEARCHER', 'ADMIN')")
    public ResponseEntity<ExperimentDTO> createExperiment(
            @Valid @RequestBody CreateExperimentRequest request,
            Authentication authentication) {
        String supabaseId = (String) authentication.getDetails();
        ExperimentDTO experiment = experimentService.createExperiment(request, supabaseId);
        return ResponseEntity.status(HttpStatus.CREATED).body(experiment);
    }

    @GetMapping
    public ResponseEntity<Page<ExperimentDTO>> getAllExperiments(Pageable pageable) {
        Page<ExperimentDTO> experiments = experimentService.getAllExperiments(pageable);
        return ResponseEntity.ok(experiments);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ExperimentDTO> getExperiment(@PathVariable Long id) {
        ExperimentDTO experiment = experimentService.getExperiment(id);
        return ResponseEntity.ok(experiment);
    }

    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('RESEARCHER', 'ADMIN')")
    public ResponseEntity<Page<ExperimentDTO>> getMyExperiments(
            Authentication authentication, Pageable pageable) {
        String supabaseId = (String) authentication.getDetails();
        return ResponseEntity.ok(experimentService.getExperimentsByOwner(supabaseId, pageable));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('RESEARCHER', 'ADMIN')")
    public ResponseEntity<ExperimentDTO> updateExperiment(
            @PathVariable Long id,
            @Valid @RequestBody UpdateExperimentRequest request,
            Authentication authentication) {
        String supabaseId = (String) authentication.getDetails();
        ExperimentDTO experiment = experimentService.updateExperiment(id, request, supabaseId);
        return ResponseEntity.ok(experiment);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('RESEARCHER', 'ADMIN')")
    public ResponseEntity<ExperimentDTO> patchStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            Authentication authentication) {
        String supabaseId = (String) authentication.getDetails();
        ExperimentStatus status = ExperimentStatus.valueOf(body.get("status"));
        UpdateExperimentRequest request = new UpdateExperimentRequest(
                null, null, null, null, null, status, null, null);
        return ResponseEntity.ok(experimentService.updateExperiment(id, request, supabaseId));
    }

    @PostMapping("/{id}/finish")
    @PreAuthorize("hasAnyRole('RESEARCHER', 'ADMIN')")
    public ResponseEntity<ExperimentDTO> finishExperiment(
            @PathVariable Long id,
            Authentication authentication) {
        String supabaseId = (String) authentication.getDetails();
        return ResponseEntity.ok(experimentService.finishExperiment(id, supabaseId));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('RESEARCHER', 'ADMIN')")
    public ResponseEntity<Void> deleteExperiment(
            @PathVariable Long id,
            Authentication authentication) {
        String supabaseId = (String) authentication.getDetails();
        experimentService.deleteExperiment(id, supabaseId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{experimentId}/participants")
    @PreAuthorize("hasAnyRole('RESEARCHER', 'ADMIN')")
    public ResponseEntity<Page<EnrollmentDTO>> getExperimentParticipants(
            @PathVariable Long experimentId, Pageable pageable) {
        return ResponseEntity.ok(enrollmentService.getExperimentEnrollments(experimentId, pageable));
    }

    @GetMapping("/{experimentId}/responses")
    @PreAuthorize("hasAnyRole('RESEARCHER', 'ADMIN')")
    public ResponseEntity<List<ResponseDTO>> getExperimentResponses(@PathVariable Long experimentId) {
        return ResponseEntity.ok(responseService.getResponsesByExperiment(experimentId));
    }

    @PostMapping("/{experimentId}/groups")
    @PreAuthorize("hasAnyRole('RESEARCHER', 'ADMIN')")
    public ResponseEntity<GroupDTO> addGroup(
            @PathVariable Long experimentId,
            @Valid @RequestBody CreateGroupRequest request,
            Authentication authentication) {
        String supabaseId = (String) authentication.getDetails();
        GroupDTO group = groupService.createGroup(experimentId, request, supabaseId);
        return ResponseEntity.status(HttpStatus.CREATED).body(group);
    }

    @GetMapping("/{experimentId}/groups")
    public ResponseEntity<List<GroupDTO>> getGroups(@PathVariable Long experimentId) {
        List<GroupDTO> groups = groupService.getGroupsByExperiment(experimentId);
        return ResponseEntity.ok(groups);
    }

    @PutMapping("/groups/{groupId}")
    @PreAuthorize("hasAnyRole('RESEARCHER', 'ADMIN')")
    public ResponseEntity<GroupDTO> updateGroup(
            @PathVariable Long groupId,
            @Valid @RequestBody CreateGroupRequest request,
            Authentication authentication) {
        String supabaseId = (String) authentication.getDetails();
        return ResponseEntity.ok(groupService.updateGroup(groupId, request, supabaseId));
    }

    @DeleteMapping("/groups/{groupId}")
    @PreAuthorize("hasAnyRole('RESEARCHER', 'ADMIN')")
    public ResponseEntity<Void> deleteGroup(
            @PathVariable Long groupId,
            Authentication authentication) {
        String supabaseId = (String) authentication.getDetails();
        groupService.deleteGroup(groupId, supabaseId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{experimentId}/phases")
    @PreAuthorize("hasAnyRole('RESEARCHER', 'ADMIN')")
    public ResponseEntity<PhaseDTO> addPhase(
            @PathVariable Long experimentId,
            @Valid @RequestBody CreatePhaseRequest request,
            Authentication authentication) {
        String supabaseId = (String) authentication.getDetails();
        PhaseDTO phase = phaseService.createPhase(experimentId, request, supabaseId);
        return ResponseEntity.status(HttpStatus.CREATED).body(phase);
    }

    @GetMapping("/{experimentId}/phases")
    public ResponseEntity<List<PhaseDTO>> getPhases(@PathVariable Long experimentId) {
        List<PhaseDTO> phases = phaseService.getPhasesByExperiment(experimentId);
        return ResponseEntity.ok(phases);
    }

    @PutMapping("/phases/{phaseId}")
    @PreAuthorize("hasAnyRole('RESEARCHER', 'ADMIN')")
    public ResponseEntity<PhaseDTO> updatePhase(
            @PathVariable Long phaseId,
            @Valid @RequestBody UpdatePhaseRequest request,
            Authentication authentication) {
        String supabaseId = (String) authentication.getDetails();
        return ResponseEntity.ok(phaseService.updatePhase(phaseId, request, supabaseId));
    }

    @DeleteMapping("/phases/{phaseId}")
    @PreAuthorize("hasAnyRole('RESEARCHER', 'ADMIN')")
    public ResponseEntity<Void> deletePhase(
            @PathVariable Long phaseId,
            Authentication authentication) {
        String supabaseId = (String) authentication.getDetails();
        phaseService.deletePhase(phaseId, supabaseId);
        return ResponseEntity.noContent().build();
    }
}
