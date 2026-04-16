package com.research.experimentplatform.service;

import com.research.experimentplatform.dto.EnrollParticipantRequest;
import com.research.experimentplatform.dto.EnrollmentDTO;
import com.research.experimentplatform.exception.ForbiddenException;
import com.research.experimentplatform.model.Enrollment;
import com.research.experimentplatform.model.EnrollmentStatus;
import com.research.experimentplatform.model.Experiment;
import com.research.experimentplatform.model.ExperimentStatus;
import com.research.experimentplatform.model.Group;
import com.research.experimentplatform.model.Participant;
import com.research.experimentplatform.model.User;
import com.research.experimentplatform.exception.BadRequestException;
import com.research.experimentplatform.exception.ConflictException;
import com.research.experimentplatform.exception.ResourceNotFoundException;
import com.research.experimentplatform.repository.EnrollmentRepository;
import com.research.experimentplatform.repository.ExperimentRepository;
import com.research.experimentplatform.repository.GroupRepository;
import com.research.experimentplatform.repository.ParticipantRepository;
import com.research.experimentplatform.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
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
    private final UserRepository userRepository;

    public EnrollmentService(EnrollmentRepository enrollmentRepository,
                             ParticipantRepository participantRepository,
                             ExperimentRepository experimentRepository,
                             GroupRepository groupRepository,
                             UserRepository userRepository) {
        this.enrollmentRepository = enrollmentRepository;
        this.participantRepository = participantRepository;
        this.experimentRepository = experimentRepository;
        this.groupRepository = groupRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public EnrollmentDTO enrollParticipant(Long participantId, EnrollParticipantRequest request) {
        // Buscamos el participante que se va a inscribir
        Participant participant = participantRepository.findById(participantId)
                .orElseThrow(() -> new ResourceNotFoundException("Participant not found"));

        // Buscamos el experimento en el que se quiere inscribir
        Experiment experiment = experimentRepository.findById(request.getExperimentId())
                .orElseThrow(() -> new ResourceNotFoundException("Experiment not found"));

        // Comprobamos que el participante no esté ya inscrito en este experimento
        // Esto evita duplicados en la mayoría de casos (aunque luego hay un try-catch por si acaso)
        if (enrollmentRepository.existsByParticipantIdAndExperimentId(participantId, request.getExperimentId())) {
            throw new ConflictException("Participant already enrolled in this experiment");
        }

        if (experiment.getStatus() != ExperimentStatus.ACTIVE) {
            throw new BadRequestException("Experiment is not active");
        }

        // Si el experimento no permite inscripción tardía, bloqueamos nuevos participantes
        if (!experiment.isAllowLateEnrollment()) {
            throw new BadRequestException("This experiment does not accept new participants after it has started. Use an invitation to join before the experiment begins.");
        }

        // Si el experimento tiene contrato de consentimiento, el participante debe aceptarlo
        if (experiment.getConsentText() != null && !experiment.getConsentText().isBlank()) {
            if (!Boolean.TRUE.equals(request.getConsentAgreed())) {
                throw new ForbiddenException("You must agree to the consent form to participate in this experiment");
            }
        }

        Enrollment enrollment = new Enrollment(participant, experiment, EnrollmentStatus.ACTIVE);

        if (experiment.getConsentText() != null && !experiment.getConsentText().isBlank()) {
            enrollment.setConsentSignedAt(LocalDateTime.now());
        }

        // Si en el request viene un grupo asignado, lo añadimos a la inscripción
        // El grupo es opcional, puede ser null
        if (request.getGroupId() != null) {
            Group group = groupRepository.findById(request.getGroupId())
                    .orElseThrow(() -> new ResourceNotFoundException("Group not found"));

            // Validamos que el grupo pertenezca al experimento correcto
            // No tiene sentido asignar un grupo de otro experimento
            if (!group.getExperiment().getId().equals(experiment.getId())) {
                throw new BadRequestException("Group does not belong to this experiment");
            }

            enrollment.setGroup(group);
        }

        // Guardamos la inscripción en la base de datos
        // El try-catch es por si dos personas intentan inscribirse a la vez (race condition)
        // La base de datos tiene una constraint UNIQUE que lo previene, pero hay que capturar la excepción
        try {
            Enrollment persisted = enrollmentRepository.save(enrollment);
            return toDTO(persisted);
        } catch (DataIntegrityViolationException e) {
            // Si salta esta excepción es porque ya existe la inscripción (constraint UNIQUE)
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
        return new EnrollmentDTO(
                enrollment.getId(),
                enrollment.getParticipant().getId(),
                enrollment.getExperiment().getId(),
                enrollment.getExperiment().getTitle(),
                enrollment.getGroup() != null ? enrollment.getGroup().getId() : null,
                enrollment.getGroup() != null ? enrollment.getGroup().getName() : null,
                enrollment.getStatus(),
                enrollment.getEnrolledAt(),
                enrollment.getCompletedAt(),
                enrollment.getConsentSignedAt()
        );
    }
}
