package com.research.experimentplatform.service;

import com.research.experimentplatform.dto.EnrollParticipantRequest;
import com.research.experimentplatform.dto.EnrollmentDTO;
import com.research.experimentplatform.exception.ForbiddenException;
import com.research.experimentplatform.model.DesignType;
import com.research.experimentplatform.model.Enrollment;
import com.research.experimentplatform.model.EnrollmentStatus;
import com.research.experimentplatform.model.Experiment;
import com.research.experimentplatform.model.ExperimentStatus;
import com.research.experimentplatform.model.Group;
import com.research.experimentplatform.model.Participant;
import com.research.experimentplatform.model.Phase;
import com.research.experimentplatform.model.User;
import com.research.experimentplatform.exception.BadRequestException;
import com.research.experimentplatform.exception.ConflictException;
import com.research.experimentplatform.exception.ResourceNotFoundException;
import com.research.experimentplatform.repository.EnrollmentRepository;
import com.research.experimentplatform.repository.ExperimentRepository;
import com.research.experimentplatform.repository.GroupRepository;
import com.research.experimentplatform.repository.ParticipantRepository;
import com.research.experimentplatform.repository.PhaseRepository;
import com.research.experimentplatform.repository.ResponseRepository;
import com.research.experimentplatform.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

@Service
public class EnrollmentService {

    private static final Map<EnrollmentStatus, Set<EnrollmentStatus>> VALID_TRANSITIONS = Map.of(
            EnrollmentStatus.PENDING, Set.of(EnrollmentStatus.ACTIVE, EnrollmentStatus.WITHDRAWN),
            EnrollmentStatus.ACTIVE, Set.of(EnrollmentStatus.COMPLETED, EnrollmentStatus.WITHDRAWN),
            EnrollmentStatus.COMPLETED, Set.of(),
            EnrollmentStatus.WITHDRAWN, Set.of()
    );

    private final EnrollmentRepository enrollmentRepository;
    private final ParticipantRepository participantRepository;
    private final ExperimentRepository experimentRepository;
    private final GroupRepository groupRepository;
    private final PhaseRepository phaseRepository;
    private final ResponseRepository responseRepository;
    private final UserRepository userRepository;

    public EnrollmentService(EnrollmentRepository enrollmentRepository,
                             ParticipantRepository participantRepository,
                             ExperimentRepository experimentRepository,
                             GroupRepository groupRepository,
                             PhaseRepository phaseRepository,
                             ResponseRepository responseRepository,
                             UserRepository userRepository) {
        this.enrollmentRepository = enrollmentRepository;
        this.participantRepository = participantRepository;
        this.experimentRepository = experimentRepository;
        this.groupRepository = groupRepository;
        this.phaseRepository = phaseRepository;
        this.responseRepository = responseRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public EnrollmentDTO enrollParticipant(Long participantId, EnrollParticipantRequest request) {
        Participant participant = participantRepository.findById(participantId)
                .orElseThrow(() -> new ResourceNotFoundException("Participant not found"));

        Experiment experiment = experimentRepository.findById(request.getExperimentId())
                .orElseThrow(() -> new ResourceNotFoundException("Experiment not found"));

        if (enrollmentRepository.existsByParticipantIdAndExperimentId(participantId, request.getExperimentId())) {
            throw new ConflictException("Participant already enrolled in this experiment");
        }

        if (experiment.getStatus() != ExperimentStatus.ACTIVE) {
            throw new BadRequestException("Experiment is not active");
        }

        if (!experiment.isAllowLateEnrollment()) {
            throw new BadRequestException("This experiment does not accept new participants after it has started. Use an invitation to join before the experiment begins.");
        }

        if (experiment.getConsentText() != null && !experiment.getConsentText().isBlank()) {
            if (!Boolean.TRUE.equals(request.getConsentAgreed())) {
                throw new ForbiddenException("You must agree to the consent form to participate in this experiment");
            }
        }

        Enrollment enrollment = new Enrollment(participant, experiment, EnrollmentStatus.ACTIVE);

        if (experiment.getConsentText() != null && !experiment.getConsentText().isBlank()) {
            enrollment.setConsentSignedAt(LocalDateTime.now());
        }

        if (request.getGroupId() != null) {
            // El investigador especificó un grupo concreto
            Group group = groupRepository.findById(request.getGroupId())
                    .orElseThrow(() -> new ResourceNotFoundException("Group not found"));

            if (!group.getExperiment().getId().equals(experiment.getId())) {
                throw new BadRequestException("Group does not belong to this experiment");
            }

            enrollment.setGroup(group);

        } else if (experiment.getDesignType() == DesignType.BETWEEN_SUBJECTS) {
            // En Between-Subjects, si no se indicó grupo, asignamos aleatoriamente
            List<Group> grupos = groupRepository.findByExperimentId(experiment.getId());

            if (!grupos.isEmpty()) {
                int indiceAleatorio = new Random().nextInt(grupos.size());
                enrollment.setGroup(grupos.get(indiceAleatorio));
            }
        }

        if (experiment.getDesignType() == DesignType.WITHIN_SUBJECTS) {
            // En Within-Subjects asignamos una secuencia de fases rotada (contrabalanceo)
            // Participante 0 → A-B-C, participante 1 → B-C-A, participante 2 → C-A-B, etc.
            List<Phase> fases = phaseRepository.findByExperimentIdOrderByPhaseOrderAsc(experiment.getId());

            if (!fases.isEmpty()) {
                // El desplazamiento depende de cuántos participantes hay ya inscritos
                int numInscritos = enrollmentRepository.findByExperimentId(experiment.getId()).size();
                int total = fases.size();

                // Construimos la secuencia rotada como cadena "id1,id2,id3"
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < total; i++) {
                    int indice = (i + numInscritos) % total;
                    if (i > 0) {
                        sb.append(",");
                    }
                    sb.append(fases.get(indice).getId());
                }

                enrollment.setPhaseSequence(sb.toString());
            }
        }

        // try-catch por si dos inscripciones llegan a la vez (la constraint UNIQUE en BD lo previene)
        try {
            Enrollment persisted = enrollmentRepository.save(enrollment);
            return toDTO(persisted);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("Participant already enrolled in this experiment");
        }
    }

    public Page<EnrollmentDTO> getParticipantEnrollments(Long participantId, Pageable pageable) {
        return enrollmentRepository.findByParticipantId(participantId, pageable)
                .map(this::toDTO);
    }

    public Page<EnrollmentDTO> getEnrollmentsByUserId(String supabaseId, Pageable pageable) {
        User user = userRepository.findBySupabaseId(supabaseId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Participant participant = participantRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Participant not found"));
        return enrollmentRepository.findByParticipantId(participant.getId(), pageable)
                .map(this::toDTO);
    }

    public EnrollmentDTO getEnrollmentById(Long enrollmentId) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found"));
        return toDTO(enrollment);
    }

    public Page<EnrollmentDTO> getExperimentEnrollments(Long experimentId, Pageable pageable) {
        return enrollmentRepository.findByExperimentId(experimentId, pageable)
                .map(this::toDTO);
    }

    @Transactional
    public EnrollmentDTO updateEnrollmentStatus(Long enrollmentId, EnrollmentStatus newStatus) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found"));

        validateStatusTransition(enrollment.getStatus(), newStatus);

        enrollment.setStatus(newStatus);

        if (newStatus == EnrollmentStatus.COMPLETED && enrollment.getCompletedAt() == null) {
            enrollment.setCompletedAt(LocalDateTime.now());
        }

        Enrollment updated = enrollmentRepository.save(enrollment);
        return toDTO(updated);
    }

    @Transactional
    public void withdrawEnrollment(Long enrollmentId) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found"));

        validateStatusTransition(enrollment.getStatus(), EnrollmentStatus.WITHDRAWN);

        enrollment.setStatus(EnrollmentStatus.WITHDRAWN);
        enrollmentRepository.save(enrollment);
    }

    @Transactional
    public EnrollmentDTO assignGroup(Long enrollmentId, Long groupId) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found"));

        if (groupId == null) {
            enrollment.setGroup(null);
        } else {
            Group group = groupRepository.findById(groupId)
                    .orElseThrow(() -> new ResourceNotFoundException("Group not found"));
            if (!group.getExperiment().getId().equals(enrollment.getExperiment().getId())) {
                throw new BadRequestException("Group does not belong to this experiment");
            }
            enrollment.setGroup(group);
        }

        return toDTO(enrollmentRepository.save(enrollment));
    }

    @Transactional
    public EnrollmentDTO completeEnrollment(Long enrollmentId, String supabaseId) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found"));

        User user = userRepository.findBySupabaseId(supabaseId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Participant participant = participantRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Participant not found"));

        if (!enrollment.getParticipant().getId().equals(participant.getId())) {
            throw new ForbiddenException("You can only complete your own enrollments");
        }

        validateStatusTransition(enrollment.getStatus(), EnrollmentStatus.COMPLETED);
        enrollment.setStatus(EnrollmentStatus.COMPLETED);
        enrollment.setCompletedAt(LocalDateTime.now());

        return toDTO(enrollmentRepository.save(enrollment));
    }

    @Transactional
    public void deleteParticipantData(Long enrollmentId) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found"));

        // Borrar todas las respuestas de este participante en este experimento
        responseRepository.deleteByEnrollmentId(enrollmentId);

        // Marcar la inscripción como retirada
        enrollment.setStatus(EnrollmentStatus.WITHDRAWN);
        enrollmentRepository.save(enrollment);
    }

    @Transactional
    public EnrollmentDTO signConsent(Long enrollmentId) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found"));

        if (enrollment.getConsentSignedAt() == null) {
            enrollment.setConsentSignedAt(LocalDateTime.now());
            enrollmentRepository.save(enrollment);
        }

        return toDTO(enrollment);
    }

    public long countActiveEnrollments(Long experimentId) {
        return enrollmentRepository.countByExperimentIdAndStatus(experimentId, EnrollmentStatus.ACTIVE);
    }

    private void validateStatusTransition(EnrollmentStatus currentStatus, EnrollmentStatus newStatus) {
        Set<EnrollmentStatus> allowed = VALID_TRANSITIONS.get(currentStatus);
        if (allowed == null || !allowed.contains(newStatus)) {
            throw new BadRequestException(
                    "Invalid status transition from " + currentStatus + " to " + newStatus);
        }
    }

    public EnrollmentDTO toDTO(Enrollment enrollment) {
        Long groupId = null;
        String groupName = null;
        if (enrollment.getGroup() != null) {
            groupId = enrollment.getGroup().getId();
            groupName = enrollment.getGroup().getName();
        }

        return new EnrollmentDTO(
                enrollment.getId(),
                enrollment.getParticipant().getId(),
                enrollment.getExperiment().getId(),
                enrollment.getExperiment().getTitle(),
                groupId,
                groupName,
                enrollment.getStatus(),
                enrollment.getEnrolledAt(),
                enrollment.getCompletedAt(),
                enrollment.getConsentSignedAt(),
                enrollment.getPhaseSequence()
        );
    }
}
