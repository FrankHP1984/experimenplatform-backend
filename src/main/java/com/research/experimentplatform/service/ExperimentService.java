package com.research.experimentplatform.service;

import com.research.experimentplatform.dto.CreateExperimentRequest;
import com.research.experimentplatform.dto.ExperimentDTO;
import com.research.experimentplatform.dto.UpdateExperimentRequest;
import com.research.experimentplatform.model.DesignType;
import com.research.experimentplatform.model.Experiment;
import com.research.experimentplatform.model.ExperimentStatus;
import com.research.experimentplatform.model.Group;
import com.research.experimentplatform.model.Phase;
import com.research.experimentplatform.model.User;
import com.research.experimentplatform.model.UserRole;
import com.research.experimentplatform.exception.BadRequestException;
import com.research.experimentplatform.exception.ForbiddenException;
import com.research.experimentplatform.exception.ResourceNotFoundException;
import com.research.experimentplatform.model.Enrollment;
import com.research.experimentplatform.model.EnrollmentStatus;
import com.research.experimentplatform.model.Organization;
import com.research.experimentplatform.model.ResearchTeam;
import com.research.experimentplatform.repository.EnrollmentRepository;
import com.research.experimentplatform.repository.ExperimentRepository;
import com.research.experimentplatform.repository.GroupRepository;
import com.research.experimentplatform.repository.OrganizationRepository;
import com.research.experimentplatform.repository.PhaseRepository;
import com.research.experimentplatform.repository.ResearchTeamRepository;
import com.research.experimentplatform.repository.UserRepository;

import java.util.List;
import com.research.experimentplatform.security.OwnershipChecker;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class ExperimentService {

    // Validaciones de transiciones de estado
    // DRAFT -> ACTIVE -> FINISHED (no se puede volver atrás)

    private final ExperimentRepository experimentRepository;
    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final PhaseRepository phaseRepository;
    private final GroupRepository groupRepository;
    private final ResearchTeamRepository researchTeamRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final OwnershipChecker ownershipChecker;

    public ExperimentService(ExperimentRepository experimentRepository,
                             UserRepository userRepository,
                             OrganizationRepository organizationRepository,
                             PhaseRepository phaseRepository,
                             GroupRepository groupRepository,
                             ResearchTeamRepository researchTeamRepository,
                             EnrollmentRepository enrollmentRepository,
                             OwnershipChecker ownershipChecker) {
        this.experimentRepository = experimentRepository;
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
        this.phaseRepository = phaseRepository;
        this.groupRepository = groupRepository;
        this.researchTeamRepository = researchTeamRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.ownershipChecker = ownershipChecker;
    }

    @Transactional
    public ExperimentDTO createExperiment(CreateExperimentRequest request, String supabaseId) {
        User owner = userRepository.findBySupabaseId(supabaseId)
                .orElseThrow(() -> new ResourceNotFoundException("Owner not found"));

        if (owner.getRole() != UserRole.RESEARCHER && owner.getRole() != UserRole.ADMIN) {
            throw new ForbiddenException("Only researchers can create experiments");
        }

        validateExperimentDates(request.startDate(), request.endDate());

        Experiment experiment = new Experiment();
        experiment.setTitle(request.title());
        experiment.setDescription(request.description());
        experiment.setDesignType(request.designType());
        experiment.setStartDate(request.startDate());
        experiment.setEndDate(request.endDate());
        experiment.setStatus(ExperimentStatus.DRAFT);
        experiment.setOwner(owner);
        experiment.setConsentText(request.consentText());
        if (request.allowLateEnrollment() != null) {
            experiment.setAllowLateEnrollment(request.allowLateEnrollment());
        }

        if (request.organizationId() != null) {
            Organization organization = organizationRepository.findById(request.organizationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));
            experiment.setOrganization(organization);
        }

        if (request.teamId() != null) {
            ResearchTeam team = researchTeamRepository.findById(request.teamId())
                    .orElseThrow(() -> new ResourceNotFoundException("Team not found"));
            if (!ownershipChecker.isMemberOf(supabaseId, team.getId())) {
                throw new ForbiddenException("You are not a member of this team");
            }
            // Si el equipo pertenece a una organización y el experimento también tiene organización,
            // ambas deben coincidir para mantener la coherencia
            if (team.getOrganization() != null && experiment.getOrganization() != null
                    && !team.getOrganization().getId().equals(experiment.getOrganization().getId())) {
                throw new BadRequestException(
                        "The team belongs to a different organization than the experiment");
            }
            experiment.setTeam(team);
        }

        Experiment created = experimentRepository.save(experiment);
        return convertToDTO(created);
    }

    public ExperimentDTO getExperiment(Long id) {
        Experiment experiment = experimentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Experiment not found"));
        return convertToDTO(experiment);
    }

    public Page<ExperimentDTO> getAllExperiments(Pageable pageable) {
        return experimentRepository.findAll(pageable)
                .map(this::convertToDTO);
    }

    public Page<ExperimentDTO> getExperimentsByOwner(String supabaseId, Pageable pageable) {
        User owner = userRepository.findBySupabaseId(supabaseId)
                .orElseThrow(() -> new ResourceNotFoundException("Owner not found"));
        return experimentRepository.findByOwnerId(owner.getId(), pageable)
                .map(this::convertToDTO);
    }

    @Transactional
    public ExperimentDTO updateExperiment(Long id, UpdateExperimentRequest request, String supabaseId) {
        Experiment experiment = experimentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Experiment not found"));

        validateOwnership(experiment, supabaseId);

        if (experiment.getStatus() == ExperimentStatus.FINISHED) {
            if (request.startDate() != null || request.endDate() != null) {
                throw new BadRequestException("Cannot change dates of a finished experiment");
            }
        }

        if (experiment.getStatus() == ExperimentStatus.ACTIVE && request.startDate() != null) {
            throw new BadRequestException("Cannot change the start date of an active experiment");
        }

        LocalDateTime effectiveStart;
        if (request.startDate() != null) {
            effectiveStart = request.startDate();
        } else {
            effectiveStart = experiment.getStartDate();
        }

        LocalDateTime effectiveEnd;
        if (request.endDate() != null) {
            effectiveEnd = request.endDate();
        } else {
            effectiveEnd = experiment.getEndDate();
        }
        validateExperimentDates(effectiveStart, effectiveEnd);

        if (request.title() != null) {
            experiment.setTitle(request.title());
        }
        if (request.description() != null) {
            experiment.setDescription(request.description());
        }
        if (request.designType() != null) {
            experiment.setDesignType(request.designType());
        }
        if (request.startDate() != null) {
            experiment.setStartDate(request.startDate());
        }
        if (request.endDate() != null) {
            experiment.setEndDate(request.endDate());
        }
        if (request.status() != null) {
            validateStatusTransition(experiment.getStatus(), request.status());
            if (request.status() == ExperimentStatus.ACTIVE) {
                validateDesignTypeForActivation(experiment);
                List<Enrollment> pending = enrollmentRepository.findByExperimentIdAndStatus(
                        experiment.getId(), EnrollmentStatus.PENDING);
                for (Enrollment e : pending) {
                    e.setStatus(EnrollmentStatus.ACTIVE);
                }
                enrollmentRepository.saveAll(pending);
            }
            experiment.setStatus(request.status());
        }
        if (request.allowLateEnrollment() != null) {
            experiment.setAllowLateEnrollment(request.allowLateEnrollment());
        }
        if (request.consentText() != null) {
            experiment.setConsentText(request.consentText());
        }
        if (request.organizationId() != null) {
            Organization organization = organizationRepository.findById(request.organizationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));
            experiment.setOrganization(organization);
        }

        if (request.teamId() != null) {
            ResearchTeam team = researchTeamRepository.findById(request.teamId())
                    .orElseThrow(() -> new ResourceNotFoundException("Team not found"));
            if (!ownershipChecker.isMemberOf(supabaseId, team.getId())) {
                throw new ForbiddenException("You are not a member of this team");
            }
            Organization experimentOrg = experiment.getOrganization();
            if (team.getOrganization() != null && experimentOrg != null
                    && !team.getOrganization().getId().equals(experimentOrg.getId())) {
                throw new BadRequestException(
                        "The team belongs to a different organization than the experiment");
            }
            experiment.setTeam(team);
        }

        Experiment result = experimentRepository.save(experiment);
        return convertToDTO(result);
    }

    @Transactional
    public ExperimentDTO finishExperiment(Long id, String supabaseId) {
        Experiment experiment = experimentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Experiment not found"));

        validateOwnership(experiment, supabaseId);

        if (experiment.getStatus() != ExperimentStatus.ACTIVE) {
            throw new BadRequestException("Only active experiments can be finished");
        }

        experiment.setStatus(ExperimentStatus.FINISHED);
        return convertToDTO(experimentRepository.save(experiment));
    }

    @Transactional
    public void deleteExperiment(Long id, String supabaseId) {
        Experiment experiment = experimentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Experiment not found"));
        validateOwnership(experiment, supabaseId);
        if (experiment.getStatus() == ExperimentStatus.ACTIVE) {
            throw new BadRequestException("Cannot delete an active experiment. Finish it first.");
        }
        experimentRepository.delete(experiment);
    }

    private void validateOwnership(Experiment experiment, String supabaseId) {
        if (!ownershipChecker.canModify(experiment, supabaseId)) {
            throw new ForbiddenException("You do not have permission to modify this experiment");
        }
    }

    private void validateDesignTypeForActivation(Experiment experiment) {
        if (experiment.getDesignType() == null) return;

        List<Phase> phases = phaseRepository.findByExperimentId(experiment.getId());
        int count = phases.size();
        DesignType type = experiment.getDesignType();

        if (type == DesignType.CROSS_SECTIONAL) {
            if (count != 1) {
                throw new BadRequestException(
                        "Un diseño transversal (Cross-Sectional) requiere exactamente 1 fase. " +
                        "Actualmente tiene " + count + ".");
            }
        } else if (type == DesignType.PRETEST_POSTTEST) {
            if (count != 2) {
                throw new BadRequestException(
                        "Un diseño Pretest-Postest requiere exactamente 2 fases (pretest y postest). " +
                        "Actualmente tiene " + count + ".");
            }
        } else if (type == DesignType.LONGITUDINAL) {
            if (count < 2) {
                throw new BadRequestException(
                        "Un diseño longitudinal requiere al menos 2 fases. " +
                        "Actualmente tiene " + count + ".");
            }
            boolean allHaveDates = phases.stream().allMatch(p -> p.getStartDate() != null);
            if (!allHaveDates) {
                throw new BadRequestException(
                        "En un diseño longitudinal todas las fases deben tener fecha de inicio configurada.");
            }
        } else if (type == DesignType.BETWEEN_SUBJECTS) {
            List<Group> groups = groupRepository.findByExperimentId(experiment.getId());
            if (groups.size() < 2) {
                throw new BadRequestException(
                        "Un diseño entre grupos requiere al menos 2 grupos. " +
                        "Actualmente tiene " + groups.size() + ".");
            }
            boolean allGroupsHavePhase = groups.stream().allMatch(g ->
                    phases.stream().anyMatch(p -> p.getGroup() != null && p.getGroup().getId().equals(g.getId())));
            if (!allGroupsHavePhase) {
                throw new BadRequestException(
                        "En un diseño entre grupos cada grupo debe tener al menos una fase asignada.");
            }
        }
    }

    private void validateExperimentDates(LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate != null && endDate != null && !endDate.isAfter(startDate)) {
            throw new BadRequestException("End date must be after start date");
        }
    }

    private void validateStatusTransition(ExperimentStatus currentStatus, ExperimentStatus newStatus) {
        if (currentStatus == newStatus) {
            return;
        }
        // Validar transiciones permitidas
        if (currentStatus == ExperimentStatus.DRAFT && newStatus != ExperimentStatus.ACTIVE) {
            throw new BadRequestException("Un experimento en borrador solo puede activarse");
        }
        
        if (currentStatus == ExperimentStatus.ACTIVE && newStatus != ExperimentStatus.FINISHED) {
            throw new BadRequestException("Un experimento activo solo puede finalizarse");
        }
        
        if (currentStatus == ExperimentStatus.FINISHED) {
            throw new BadRequestException("No se puede cambiar el estado de un experimento finalizado");
        }
    }

    public ExperimentDTO convertToDTO(Experiment experiment) {
        Long organizationId = null;
        String organizationName = null;
        if (experiment.getOrganization() != null) {
            organizationId = experiment.getOrganization().getId();
            organizationName = experiment.getOrganization().getName();
        }

        Long teamId = null;
        String teamName = null;
        if (experiment.getTeam() != null) {
            teamId = experiment.getTeam().getId();
            teamName = experiment.getTeam().getName();
        }

        return new ExperimentDTO(
                experiment.getId(),
                experiment.getTitle(),
                experiment.getDescription(),
                experiment.getDesignType(),
                experiment.getStartDate(),
                experiment.getEndDate(),
                experiment.getStatus(),
                experiment.getOwner().getId(),
                experiment.getConsentText(),
                organizationId,
                organizationName,
                teamId,
                teamName,
                experiment.isAllowLateEnrollment()
        );
    }
}
