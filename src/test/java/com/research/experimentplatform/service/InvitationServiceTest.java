package com.research.experimentplatform.service;

import com.research.experimentplatform.dto.AcceptInvitationRequest;
import com.research.experimentplatform.dto.CreateInvitationRequest;
import com.research.experimentplatform.dto.EnrollmentDTO;
import com.research.experimentplatform.dto.InvitationDTO;
import com.research.experimentplatform.exception.BadRequestException;
import com.research.experimentplatform.exception.ConflictException;
import com.research.experimentplatform.exception.ForbiddenException;
import com.research.experimentplatform.model.Enrollment;
import com.research.experimentplatform.model.Experiment;
import com.research.experimentplatform.model.ExperimentStatus;
import com.research.experimentplatform.model.Invitation;
import com.research.experimentplatform.model.InvitationStatus;
import com.research.experimentplatform.model.Participant;
import com.research.experimentplatform.model.User;
import com.research.experimentplatform.model.UserRole;
import com.research.experimentplatform.repository.EnrollmentRepository;
import com.research.experimentplatform.repository.ExperimentRepository;
import com.research.experimentplatform.repository.GroupRepository;
import com.research.experimentplatform.repository.InvitationRepository;
import com.research.experimentplatform.repository.ParticipantRepository;
import com.research.experimentplatform.repository.UserRepository;
import com.research.experimentplatform.security.OwnershipChecker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvitationServiceTest {

    @Mock
    private InvitationRepository invitationRepository;
    @Mock
    private ExperimentRepository experimentRepository;
    @Mock
    private GroupRepository groupRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ParticipantRepository participantRepository;
    @Mock
    private EnrollmentRepository enrollmentRepository;
    @Mock
    private EnrollmentService enrollmentService;
    @Mock
    private OwnershipChecker ownershipChecker;

    @InjectMocks
    private InvitationService invitationService;

    private User investigador;
    private User participanteUser;
    private Participant participante;
    private Experiment experimento;

    @BeforeEach
    void setUp() {
        investigador = new User("supa-inv-1", "investigador@test.com", UserRole.RESEARCHER);
        investigador.setId(1L);

        participanteUser = new User("supa-part-1", "participante@test.com", UserRole.PARTICIPANT);
        participanteUser.setId(2L);

        participante = new Participant(participanteUser);
        participante.setId(1L);

        experimento = new Experiment();
        experimento.setId(1L);
        experimento.setTitle("Experimento A");
        experimento.setStatus(ExperimentStatus.ACTIVE);
        experimento.setOwner(investigador);
        experimento.setConsentText("Acepto participar en este estudio.");
    }

    // --- createInvitation ---

    @Test
    void createInvitation_datosCorrectos_creaInvitacion() {
        CreateInvitationRequest request = new CreateInvitationRequest("participante@test.com", null);

        when(experimentRepository.findById(1L)).thenReturn(Optional.of(experimento));
        when(ownershipChecker.canModify(experimento, "supa-inv-1")).thenReturn(true);
        when(invitationRepository.existsByExperimentIdAndInvitedEmailAndStatus(
                1L, "participante@test.com", InvitationStatus.PENDING)).thenReturn(false);
        when(invitationRepository.save(any(Invitation.class))).thenAnswer(i -> {
            Invitation inv = i.getArgument(0);
            inv.setId(1L);
            return inv;
        });

        InvitationDTO result = invitationService.createInvitation(1L, request, "supa-inv-1");

        assertEquals("participante@test.com", result.getInvitedEmail());
        assertEquals(InvitationStatus.PENDING, result.getStatus());
    }

    @Test
    void createInvitation_noEsElDueno_lanzaForbidden() {
        CreateInvitationRequest request = new CreateInvitationRequest("otro@test.com", null);

        when(experimentRepository.findById(1L)).thenReturn(Optional.of(experimento));
        when(ownershipChecker.canModify(experimento, "supa-otro")).thenReturn(false);

        // "supa-otro" no es el dueño del experimento
        assertThrows(ForbiddenException.class, () ->
                invitationService.createInvitation(1L, request, "supa-otro"));
    }

    @Test
    void createInvitation_yaExisteInvitacionPendiente_lanzaConflict() {
        CreateInvitationRequest request = new CreateInvitationRequest("participante@test.com", null);

        when(experimentRepository.findById(1L)).thenReturn(Optional.of(experimento));
        when(ownershipChecker.canModify(experimento, "supa-inv-1")).thenReturn(true);
        when(invitationRepository.existsByExperimentIdAndInvitedEmailAndStatus(
                1L, "participante@test.com", InvitationStatus.PENDING)).thenReturn(true);

        assertThrows(ConflictException.class, () ->
                invitationService.createInvitation(1L, request, "supa-inv-1"));
    }

    @Test
    void createInvitation_experimentoFinalizado_lanzaBadRequest() {
        experimento.setStatus(ExperimentStatus.FINISHED);
        CreateInvitationRequest request = new CreateInvitationRequest("participante@test.com", null);

        when(experimentRepository.findById(1L)).thenReturn(Optional.of(experimento));
        when(ownershipChecker.canModify(experimento, "supa-inv-1")).thenReturn(true);

        assertThrows(BadRequestException.class, () ->
                invitationService.createInvitation(1L, request, "supa-inv-1"));
    }

    // --- acceptInvitation ---

    @Test
    void acceptInvitation_datosCorrectos_creaInscripcion() {
        Invitation invitacion = new Invitation(experimento, "participante@test.com", null,
                LocalDateTime.now().plusDays(7));
        invitacion.setId(1L);
        invitacion.setStatus(InvitationStatus.PENDING);
        invitacion.setToken("token-valido");

        AcceptInvitationRequest request = new AcceptInvitationRequest(true, null, null, null, null);

        when(invitationRepository.findByToken("token-valido")).thenReturn(Optional.of(invitacion));
        when(userRepository.findBySupabaseId("supa-part-1")).thenReturn(Optional.of(participanteUser));
        when(participantRepository.findByUserId(2L)).thenReturn(Optional.of(participante));
        when(enrollmentRepository.existsByParticipantIdAndExperimentId(1L, 1L)).thenReturn(false);
        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(i -> {
            Enrollment e = i.getArgument(0);
            e.setId(10L);
            return e;
        });
        when(invitationRepository.save(any(Invitation.class))).thenAnswer(i -> i.getArgument(0));
        when(enrollmentService.toDTO(any(Enrollment.class))).thenReturn(new EnrollmentDTO());

        EnrollmentDTO result = invitationService.acceptInvitation("token-valido", request, "supa-part-1");

        assertNotNull(result);
        assertEquals(InvitationStatus.ACCEPTED, invitacion.getStatus());
    }

    @Test
    void acceptInvitation_sinAceptarContrato_lanzaForbidden() {
        Invitation invitacion = new Invitation(experimento, "participante@test.com", null,
                LocalDateTime.now().plusDays(7));
        invitacion.setStatus(InvitationStatus.PENDING);
        invitacion.setToken("token-valido");

        // consentAgreed = false
        AcceptInvitationRequest request = new AcceptInvitationRequest(false, null, null, null, null);

        when(invitationRepository.findByToken("token-valido")).thenReturn(Optional.of(invitacion));

        assertThrows(ForbiddenException.class, () ->
                invitationService.acceptInvitation("token-valido", request, "supa-part-1"));
    }

    @Test
    void acceptInvitation_emailDistintoAlInvitado_lanzaForbidden() {
        Invitation invitacion = new Invitation(experimento, "participante@test.com", null,
                LocalDateTime.now().plusDays(7));
        invitacion.setStatus(InvitationStatus.PENDING);
        invitacion.setToken("token-valido");

        AcceptInvitationRequest request = new AcceptInvitationRequest(true, null, null, null, null);

        User otroUser = new User("supa-otro", "otro@test.com", UserRole.PARTICIPANT);
        otroUser.setId(99L);

        when(invitationRepository.findByToken("token-valido")).thenReturn(Optional.of(invitacion));
        when(userRepository.findBySupabaseId("supa-otro")).thenReturn(Optional.of(otroUser));

        // "otro@test.com" intenta aceptar una invitación enviada a "participante@test.com"
        assertThrows(ForbiddenException.class, () ->
                invitationService.acceptInvitation("token-valido", request, "supa-otro"));
    }

    @Test
    void acceptInvitation_invitacionExpirada_lanzaBadRequest() {
        Invitation invitacion = new Invitation(experimento, "participante@test.com", null,
                LocalDateTime.now().minusDays(1)); // ya expiró
        invitacion.setStatus(InvitationStatus.PENDING);
        invitacion.setToken("token-expirado");

        AcceptInvitationRequest request = new AcceptInvitationRequest(true, null, null, null, null);

        when(invitationRepository.findByToken("token-expirado")).thenReturn(Optional.of(invitacion));
        when(invitationRepository.save(any(Invitation.class))).thenAnswer(i -> i.getArgument(0));

        assertThrows(BadRequestException.class, () ->
                invitationService.acceptInvitation("token-expirado", request, "supa-part-1"));
    }

    @Test
    void acceptInvitation_invitacionYaAceptada_lanzaConflict() {
        Invitation invitacion = new Invitation(experimento, "participante@test.com", null,
                LocalDateTime.now().plusDays(7));
        invitacion.setStatus(InvitationStatus.ACCEPTED);
        invitacion.setToken("token-usado");

        AcceptInvitationRequest request = new AcceptInvitationRequest(true, null, null, null, null);

        when(invitationRepository.findByToken("token-usado")).thenReturn(Optional.of(invitacion));

        assertThrows(ConflictException.class, () ->
                invitationService.acceptInvitation("token-usado", request, "supa-part-1"));
    }

    // --- declineInvitation ---

    @Test
    void declineInvitation_datosCorrectos_cambiaEstadoADeclined() {
        Invitation invitacion = new Invitation(experimento, "participante@test.com", null,
                LocalDateTime.now().plusDays(7));
        invitacion.setStatus(InvitationStatus.PENDING);
        invitacion.setToken("token-valido");

        when(invitationRepository.findByToken("token-valido")).thenReturn(Optional.of(invitacion));
        when(userRepository.findBySupabaseId("supa-part-1")).thenReturn(Optional.of(participanteUser));
        when(invitationRepository.save(any(Invitation.class))).thenAnswer(i -> i.getArgument(0));

        invitationService.declineInvitation("token-valido", "supa-part-1");

        assertEquals(InvitationStatus.DECLINED, invitacion.getStatus());
    }

    @Test
    void declineInvitation_emailDistintoAlInvitado_lanzaForbidden() {
        Invitation invitacion = new Invitation(experimento, "participante@test.com", null,
                LocalDateTime.now().plusDays(7));
        invitacion.setStatus(InvitationStatus.PENDING);
        invitacion.setToken("token-valido");

        User otroUser = new User("supa-otro", "otro@test.com", UserRole.PARTICIPANT);
        otroUser.setId(99L);

        when(invitationRepository.findByToken("token-valido")).thenReturn(Optional.of(invitacion));
        when(userRepository.findBySupabaseId("supa-otro")).thenReturn(Optional.of(otroUser));

        assertThrows(ForbiddenException.class, () ->
                invitationService.declineInvitation("token-valido", "supa-otro"));
    }
}
