package com.research.experimentplatform.service;

import com.research.experimentplatform.dto.CreatePhaseRequest;
import com.research.experimentplatform.dto.PhaseDTO;
import com.research.experimentplatform.dto.UpdatePhaseRequest;
import com.research.experimentplatform.exception.BadRequestException;
import com.research.experimentplatform.model.Experiment;
import com.research.experimentplatform.model.Group;
import com.research.experimentplatform.model.Phase;
import com.research.experimentplatform.exception.ForbiddenException;
import com.research.experimentplatform.exception.ResourceNotFoundException;
import com.research.experimentplatform.repository.ExperimentRepository;
import com.research.experimentplatform.repository.GroupRepository;
import com.research.experimentplatform.repository.PhaseRepository;
import com.research.experimentplatform.repository.QuestionRepository;
import com.research.experimentplatform.security.OwnershipChecker;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PhaseService {

    private final PhaseRepository phaseRepository;
    private final ExperimentRepository experimentRepository;
    private final GroupRepository groupRepository;
    private final QuestionRepository questionRepository;
    private final OwnershipChecker ownershipChecker;

    public PhaseService(PhaseRepository phaseRepository, ExperimentRepository experimentRepository,
                        GroupRepository groupRepository, QuestionRepository questionRepository,
                        OwnershipChecker ownershipChecker) {
        this.phaseRepository = phaseRepository;
        this.experimentRepository = experimentRepository;
        this.groupRepository = groupRepository;
        this.questionRepository = questionRepository;
        this.ownershipChecker = ownershipChecker;
    }

    @Transactional
    public PhaseDTO createPhase(Long experimentId, CreatePhaseRequest request, String supabaseId) {
        Experiment experiment = experimentRepository.findById(experimentId)
                .orElseThrow(() -> new ResourceNotFoundException("Experiment not found"));

        if (!ownershipChecker.canModify(experiment, supabaseId)) {
            throw new ForbiddenException("You do not have permission to modify this experiment");
        }

        validatePhaseDates(experiment, request);
        validatePhaseWithinExperimentRange(experiment, request);
        validatePhaseOrderUnique(experiment.getId(), request.getPhaseOrder(), null);
        validateNoOverlap(experiment.getId(), request.getStartDate(), request.getEndDate(), null);

        Phase phase = new Phase();
        phase.setName(request.getName());
        phase.setPhaseOrder(request.getPhaseOrder());
        phase.setStartDate(request.getStartDate());
        phase.setEndDate(request.getEndDate());
        phase.setExperiment(experiment);

        if (request.getGroupId() != null) {
            Group group = groupRepository.findById(request.getGroupId())
                    .orElseThrow(() -> new ResourceNotFoundException("Group not found"));
            if (!group.getExperiment().getId().equals(experimentId)) {
                throw new BadRequestException("Group does not belong to this experiment");
            }
            phase.setGroup(group);
        }

        Phase savedPhase = phaseRepository.save(phase);
        return convertToDTO(savedPhase);
    }

    public List<PhaseDTO> getPhasesByExperiment(Long experimentId) {
        return phaseRepository.findByExperimentIdOrderByPhaseOrderAsc(experimentId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public PhaseDTO getPhase(Long id) {
        Phase phase = phaseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Phase not found"));
        return convertToDTO(phase);
    }

    @Transactional
    public PhaseDTO updatePhase(Long phaseId, UpdatePhaseRequest request, String supabaseId) {
        Phase phase = phaseRepository.findById(phaseId)
                .orElseThrow(() -> new ResourceNotFoundException("Phase not found"));

        if (!ownershipChecker.canModify(phase.getExperiment(), supabaseId)) {
            throw new ForbiddenException("You do not have permission to modify this experiment");
        }

        Experiment experiment = phase.getExperiment();

        if (request.getStartDate() != null && request.getEndDate() != null
                && !request.getEndDate().isAfter(request.getStartDate())) {
            throw new BadRequestException("La fecha de fin debe ser posterior a la de inicio");
        }

        if (experiment.getStartDate() != null && request.getStartDate() != null
                && request.getStartDate().isBefore(experiment.getStartDate())) {
            throw new BadRequestException("La fase no puede empezar antes que el experimento");
        }
        if (experiment.getEndDate() != null && request.getEndDate() != null
                && request.getEndDate().isAfter(experiment.getEndDate())) {
            throw new BadRequestException("La fase no puede terminar después del experimento");
        }

        int newOrder = request.getPhaseOrder() != null ? request.getPhaseOrder() : phase.getPhaseOrder();
        if (newOrder != phase.getPhaseOrder()) {
            validatePhaseOrderUnique(experiment.getId(), newOrder, phaseId);
        }

        validateNoOverlap(experiment.getId(), request.getStartDate(), request.getEndDate(), phaseId);

        phase.setName(request.getName());
        phase.setPhaseOrder(newOrder);
        phase.setStartDate(request.getStartDate());
        phase.setEndDate(request.getEndDate());

        if (request.isClearGroup()) {
            phase.setGroup(null);
        } else if (request.getGroupId() != null) {
            Group group = groupRepository.findById(request.getGroupId())
                    .orElseThrow(() -> new ResourceNotFoundException("Group not found"));
            if (!group.getExperiment().getId().equals(phase.getExperiment().getId())) {
                throw new BadRequestException("Group does not belong to this experiment");
            }
            phase.setGroup(group);
        }

        return convertToDTO(phaseRepository.save(phase));
    }

    @Transactional
    public void deletePhase(Long id, String supabaseId) {
        Phase phase = phaseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Phase not found"));
        if (!ownershipChecker.canModify(phase.getExperiment(), supabaseId)) {
            throw new ForbiddenException("You do not have permission to modify this experiment");
        }
        phaseRepository.delete(phase);
    }

    // Para LONGITUDINAL y PRETEST_POSTTEST la fecha de inicio es obligatoria.
    // La fecha de fin es siempre opcional: el investigador puede dejar la fase abierta
    // y cerrarla manualmente cuando finalice el experimento.
    private void validatePhaseDates(Experiment experiment, CreatePhaseRequest request) {
        if (request.getStartDate() != null && request.getEndDate() != null
                && !request.getEndDate().isAfter(request.getStartDate())) {
            throw new BadRequestException("La fecha de fin debe ser posterior a la de inicio");
        }
    }

    // Las fechas de la fase deben estar dentro del rango del experimento (si este tiene fechas definidas).
    private void validatePhaseWithinExperimentRange(Experiment experiment, CreatePhaseRequest request) {
        if (experiment.getStartDate() != null && request.getStartDate() != null
                && request.getStartDate().isBefore(experiment.getStartDate())) {
            throw new BadRequestException(
                    "La fecha de inicio de la fase no puede ser anterior a la del experimento (" + experiment.getStartDate() + ")");
        }
        if (experiment.getEndDate() != null && request.getEndDate() != null
                && request.getEndDate().isAfter(experiment.getEndDate())) {
            throw new BadRequestException(
                    "La fecha de fin de la fase no puede ser posterior a la del experimento (" + experiment.getEndDate() + ")");
        }
    }

    // Cada fase dentro de un experimento debe tener un orden único para evitar ambigüedad.
    private void validatePhaseOrderUnique(Long experimentId, Integer phaseOrder, Long excludePhaseId) {
        List<Phase> existing = phaseRepository.findByExperimentId(experimentId);
        for (Phase p : existing) {
            if (excludePhaseId != null && p.getId().equals(excludePhaseId)) continue;
            if (p.getPhaseOrder().equals(phaseOrder)) {
                throw new BadRequestException(
                        "A phase with order " + phaseOrder + " already exists in this experiment");
            }
        }
    }

    // Comprueba que la nueva fase no se solape con las ya existentes.
    // Solo se comprueba cuando ambas fases tienen endDate definido.
    // excludePhaseId sirve para ignorar la propia fase al actualizar (de momento no se usa pero evita falsos positivos futuros).
    private void validateNoOverlap(Long experimentId, LocalDateTime newStart, LocalDateTime newEnd, Long excludePhaseId) {
        if (newStart == null || newEnd == null) return;

        List<Phase> existing = phaseRepository.findByExperimentId(experimentId);
        for (Phase p : existing) {
            if (excludePhaseId != null && p.getId().equals(excludePhaseId)) continue;
            if (p.getStartDate() == null || p.getEndDate() == null) continue;

            boolean overlaps = newStart.isBefore(p.getEndDate()) && newEnd.isAfter(p.getStartDate());
            if (overlaps) {
                throw new BadRequestException(
                        "Phase dates overlap with existing phase '" + p.getName() + "'");
            }
        }
    }

    public PhaseDTO convertToDTO(Phase phase) {
        return new PhaseDTO(
                phase.getId(),
                phase.getName(),
                phase.getPhaseOrder(),
                phase.getStartDate(),
                phase.getEndDate(),
                phase.getExperiment().getId(),
                questionRepository.countByPhaseId(phase.getId()),
                phase.getGroup() != null ? phase.getGroup().getId() : null,
                phase.getGroup() != null ? phase.getGroup().getName() : null
        );
    }
}
