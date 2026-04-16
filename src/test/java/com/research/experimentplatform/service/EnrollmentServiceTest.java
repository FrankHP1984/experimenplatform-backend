package com.research.experimentplatform.service;

import com.research.experimentplatform.dto.EnrollParticipantRequest;
import com.research.experimentplatform.dto.EnrollmentDTO;
import com.research.experimentplatform.exception.BadRequestException;
import com.research.experimentplatform.exception.ConflictException;
import com.research.experimentplatform.exception.ForbiddenException;
import com.research.experimentplatform.model.Enrollment;
import com.research.experimentplatform.model.EnrollmentStatus;
import com.research.experimentplatform.model.Experiment;
import com.research.experimentplatform.model.ExperimentStatus;
import com.research.experimentplatform.model.Group;
import com.research.experimentplatform.model.Participant;
import com.research.experimentplatform.model.User;
import com.research.experimentplatform.repository.EnrollmentRepository;
import com.research.experimentplatform.repository.ExperimentRepository;
import com.research.experimentplatform.repository.GroupRepository;
import com.research.experimentplatform.repository.ParticipantRepository;
import com.research.experimentplatform.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnrollmentServiceTest {

    @Mock
    private EnrollmentRepository enrollmentRepository;
    @Mock
    private ParticipantRepository participantRepository;
    @Mock
    private ExperimentRepository experimentRepository;
    @Mock
    private GroupRepository groupRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private EnrollmentService enrollmentService;

    private Participant participant;
    private Experiment experiment;

    @BeforeEach
    void setUp() {
        User user = new User("supabase-123", "test@test.com", com.research.experimentplatform.model.UserRole.PARTICIPANT);
        user.setId(1L);

        participant = new Participant(user);
        participant.setId(1L);

        experiment = new Experiment();
        experiment.setId(1L);
        experiment.setTitle("Experimento test");
        experiment.setStatus(ExperimentStatus.ACTIVE);
        experiment.setOwner(user);
        // Los tests de esta clase prueban la lógica de inscripción en sí,
        // no la restricción de entrada tardía, así que la habilitamos por defecto.
        experiment.setAllowLateEnrollment(true);
    }

    // --- enrollParticipant ---

    @Test
    void enrollParticipant_sinContrato_creaInscripcionActiva() {
        // El experimento no tiene contrato de consentimiento
        experiment.setConsentText(null);

        EnrollParticipantRequest request = new EnrollParticipantRequest();
        request.setExperimentId(1L);

        when(participantRepository.findById(1L)).thenReturn(Optional.of(participant));
        when(experimentRepository.findById(1L)).thenReturn(Optional.of(experiment));
        when(enrollmentRepository.existsByParticipantIdAndExperimentId(1L, 1L)).thenReturn(false);
        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(i -> {
            Enrollment e = i.getArgument(0);
            e.setId(10L);
            return e;
        });

        EnrollmentDTO result = enrollmentService.enrollParticipant(1L, request);

        assertEquals(EnrollmentStatus.ACTIVE, result.getStatus());
    }

    @Test
    void enrollParticipant_conContrato_yConsentimientoAceptado_creaInscripcion() {
        experiment.setConsentText("Este es el contrato de participación.");

        EnrollParticipantRequest request = new EnrollParticipantRequest();
        request.setExperimentId(1L);
        request.setConsentAgreed(true);

        when(participantRepository.findById(1L)).thenReturn(Optional.of(participant));
        when(experimentRepository.findById(1L)).thenReturn(Optional.of(experiment));
        when(enrollmentRepository.existsByParticipantIdAndExperimentId(1L, 1L)).thenReturn(false);
        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(i -> {
            Enrollment e = i.getArgument(0);
            e.setId(10L);
            return e;
        });

        EnrollmentDTO result = enrollmentService.enrollParticipant(1L, request);

        assertEquals(EnrollmentStatus.ACTIVE, result.getStatus());
        assertNotNull(result.getConsentSignedAt());
    }

    @Test
    void enrollParticipant_conContrato_sinConsentimiento_lanzaForbidden() {
        experiment.setConsentText("Este es el contrato de participación.");

        EnrollParticipantRequest request = new EnrollParticipantRequest();
        request.setExperimentId(1L);
        request.setConsentAgreed(false);

        when(participantRepository.findById(1L)).thenReturn(Optional.of(participant));
        when(experimentRepository.findById(1L)).thenReturn(Optional.of(experiment));
        when(enrollmentRepository.existsByParticipantIdAndExperimentId(1L, 1L)).thenReturn(false);

        assertThrows(ForbiddenException.class, () ->
                enrollmentService.enrollParticipant(1L, request));
    }

    @Test
    void enrollParticipant_sinPermitirEntradaTardia_lanzaBadRequest() {
        // El experimento ya está activo pero no acepta nuevos participantes
        experiment.setAllowLateEnrollment(false);

        EnrollParticipantRequest request = new EnrollParticipantRequest();
        request.setExperimentId(1L);

        when(participantRepository.findById(1L)).thenReturn(Optional.of(participant));
        when(experimentRepository.findById(1L)).thenReturn(Optional.of(experiment));
        when(enrollmentRepository.existsByParticipantIdAndExperimentId(1L, 1L)).thenReturn(false);

        assertThrows(BadRequestException.class, () ->
                enrollmentService.enrollParticipant(1L, request));
    }

    @Test
    void enrollParticipant_experimentoNoActivo_lanzaBadRequest() {
        experiment.setStatus(ExperimentStatus.DRAFT);

        EnrollParticipantRequest request = new EnrollParticipantRequest();
        request.setExperimentId(1L);

        when(participantRepository.findById(1L)).thenReturn(Optional.of(participant));
        when(experimentRepository.findById(1L)).thenReturn(Optional.of(experiment));
        when(enrollmentRepository.existsByParticipantIdAndExperimentId(1L, 1L)).thenReturn(false);

        assertThrows(BadRequestException.class, () ->
                enrollmentService.enrollParticipant(1L, request));
    }

    @Test
    void enrollParticipant_participanteYaInscrito_lanzaConflict() {
        EnrollParticipantRequest request = new EnrollParticipantRequest();
        request.setExperimentId(1L);

        when(participantRepository.findById(1L)).thenReturn(Optional.of(participant));
        when(experimentRepository.findById(1L)).thenReturn(Optional.of(experiment));
        when(enrollmentRepository.existsByParticipantIdAndExperimentId(1L, 1L)).thenReturn(true);

        assertThrows(ConflictException.class, () ->
                enrollmentService.enrollParticipant(1L, request));
    }

    @Test
    void enrollParticipant_grupoDeOtroExperimento_lanzaBadRequest() {
        Experiment otroExperimento = new Experiment();
        otroExperimento.setId(99L);

        Group grupoDiferente = new Group("Control", "desc", otroExperimento);
        grupoDiferente.setId(5L);

        EnrollParticipantRequest request = new EnrollParticipantRequest();
        request.setExperimentId(1L);
        request.setGroupId(5L);

        when(participantRepository.findById(1L)).thenReturn(Optional.of(participant));
        when(experimentRepository.findById(1L)).thenReturn(Optional.of(experiment));
        when(enrollmentRepository.existsByParticipantIdAndExperimentId(1L, 1L)).thenReturn(false);
        when(groupRepository.findById(5L)).thenReturn(Optional.of(grupoDiferente));

        assertThrows(BadRequestException.class, () ->
                enrollmentService.enrollParticipant(1L, request));
    }

    // --- updateEnrollmentStatus ---

    @Test
    void updateEnrollmentStatus_transicionValida_actualizaEstado() {
        Enrollment enrollment = new Enrollment(participant, experiment, EnrollmentStatus.PENDING);
        enrollment.setId(1L);

        when(enrollmentRepository.findById(1L)).thenReturn(Optional.of(enrollment));
        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(i -> i.getArgument(0));

        EnrollmentDTO result = enrollmentService.updateEnrollmentStatus(1L, EnrollmentStatus.ACTIVE);

        assertEquals(EnrollmentStatus.ACTIVE, result.getStatus());
    }

    @Test
    void updateEnrollmentStatus_transicionInvalida_lanzaBadRequest() {
        Enrollment enrollment = new Enrollment(participant, experiment, EnrollmentStatus.COMPLETED);
        enrollment.setId(1L);

        when(enrollmentRepository.findById(1L)).thenReturn(Optional.of(enrollment));

        // No se puede volver de COMPLETED a ACTIVE
        assertThrows(BadRequestException.class, () ->
                enrollmentService.updateEnrollmentStatus(1L, EnrollmentStatus.ACTIVE));
    }

    @Test
    void updateEnrollmentStatus_aCompleted_guardaFechaCompletado() {
        Enrollment enrollment = new Enrollment(participant, experiment, EnrollmentStatus.ACTIVE);
        enrollment.setId(1L);

        when(enrollmentRepository.findById(1L)).thenReturn(Optional.of(enrollment));
        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(i -> i.getArgument(0));

        EnrollmentDTO result = enrollmentService.updateEnrollmentStatus(1L, EnrollmentStatus.COMPLETED);

        assertEquals(EnrollmentStatus.COMPLETED, result.getStatus());
        assertNotNull(result.getCompletedAt());
    }

    // --- withdrawEnrollment ---

    @Test
    void withdrawEnrollment_inscripcionActiva_cambiaEstadoAWithdrawn() {
        Enrollment enrollment = new Enrollment(participant, experiment, EnrollmentStatus.ACTIVE);
        enrollment.setId(1L);

        when(enrollmentRepository.findById(1L)).thenReturn(Optional.of(enrollment));
        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(i -> i.getArgument(0));

        // No lanza excepción y la inscripción pasa a WITHDRAWN
        enrollmentService.withdrawEnrollment(1L);

        assertEquals(EnrollmentStatus.WITHDRAWN, enrollment.getStatus());
    }

    @Test
    void withdrawEnrollment_inscripcionCompletada_lanzaBadRequest() {
        Enrollment enrollment = new Enrollment(participant, experiment, EnrollmentStatus.COMPLETED);
        enrollment.setId(1L);

        when(enrollmentRepository.findById(1L)).thenReturn(Optional.of(enrollment));

        assertThrows(BadRequestException.class, () ->
                enrollmentService.withdrawEnrollment(1L));
    }
}
