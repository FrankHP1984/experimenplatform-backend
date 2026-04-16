package com.research.experimentplatform.controller;

import com.research.experimentplatform.dto.CreateOrganizationRequest;
import com.research.experimentplatform.dto.OrganizationDTO;
import com.research.experimentplatform.service.OrganizationService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/organizations")
public class OrganizationController {

    private final OrganizationService organizationService;

    public OrganizationController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OrganizationDTO> createOrganization(
            @Valid @RequestBody CreateOrganizationRequest request) {
        OrganizationDTO organization = organizationService.createOrganization(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(organization);
    }

    @GetMapping
    public ResponseEntity<Page<OrganizationDTO>> getAllOrganizations(Pageable pageable) {
        Page<OrganizationDTO> organizations = organizationService.getAllOrganizations(pageable);
        return ResponseEntity.ok(organizations);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrganizationDTO> getOrganization(@PathVariable Long id) {
        OrganizationDTO organization = organizationService.getOrganization(id);
        return ResponseEntity.ok(organization);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OrganizationDTO> updateOrganization(
            @PathVariable Long id,
            @Valid @RequestBody CreateOrganizationRequest request) {
        OrganizationDTO organization = organizationService.updateOrganization(id, request);
        return ResponseEntity.ok(organization);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteOrganization(@PathVariable Long id) {
        organizationService.deleteOrganization(id);
        return ResponseEntity.noContent().build();
    }
}
