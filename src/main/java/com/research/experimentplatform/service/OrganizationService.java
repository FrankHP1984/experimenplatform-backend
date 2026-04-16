package com.research.experimentplatform.service;

import com.research.experimentplatform.dto.CreateOrganizationRequest;
import com.research.experimentplatform.dto.OrganizationDTO;
import com.research.experimentplatform.exception.BadRequestException;
import com.research.experimentplatform.exception.ResourceNotFoundException;
import com.research.experimentplatform.model.Organization;
import com.research.experimentplatform.repository.OrganizationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrganizationService {

    private final OrganizationRepository organizationRepository;

    public OrganizationService(OrganizationRepository organizationRepository) {
        this.organizationRepository = organizationRepository;
    }

    @Transactional
    public OrganizationDTO createOrganization(CreateOrganizationRequest request) {
        if (organizationRepository.existsByName(request.name())) {
            throw new BadRequestException("An organization with this name already exists");
        }

        Organization organization = new Organization(
                request.name(),
                request.description(),
                request.website(),
                request.type()
        );

        Organization saved = organizationRepository.save(organization);
        return convertToDTO(saved);
    }

    public OrganizationDTO getOrganization(Long id) {
        Organization organization = organizationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));
        return convertToDTO(organization);
    }

    public Page<OrganizationDTO> getAllOrganizations(Pageable pageable) {
        return organizationRepository.findAll(pageable)
                .map(this::convertToDTO);
    }

    @Transactional
    public OrganizationDTO updateOrganization(Long id, CreateOrganizationRequest request) {
        Organization organization = organizationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));

        if (!organization.getName().equals(request.name())
                && organizationRepository.existsByName(request.name())) {
            throw new BadRequestException("An organization with this name already exists");
        }

        organization.setName(request.name());
        organization.setDescription(request.description());
        organization.setWebsite(request.website());
        organization.setType(request.type());

        Organization updated = organizationRepository.save(organization);
        return convertToDTO(updated);
    }

    @Transactional
    public void deleteOrganization(Long id) {
        Organization organization = organizationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));

        if (!organization.getTeams().isEmpty()) {
            throw new BadRequestException(
                    "Cannot delete organization with active teams. Remove teams first.");
        }
        if (!organization.getExperiments().isEmpty()) {
            throw new BadRequestException(
                    "Cannot delete organization with associated experiments. Reassign or delete experiments first.");
        }

        organizationRepository.delete(organization);
    }

    public OrganizationDTO convertToDTO(Organization organization) {
        return new OrganizationDTO(
                organization.getId(),
                organization.getName(),
                organization.getDescription(),
                organization.getWebsite(),
                organization.getType(),
                organization.getCreatedAt()
        );
    }
}
