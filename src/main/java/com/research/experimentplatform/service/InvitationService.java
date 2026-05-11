package com.research.experimentplatform.service;

import com.research.experimentplatform.dto.AcceptInvitationRequest;
import com.research.experimentplatform.dto.CreateInvitationRequest;
import com.research.experimentplatform.dto.EnrollmentDTO;
import com.research.experimentplatform.dto.InvitationDTO;
import com.research.experimentplatform.exception.BadRequestException;
import com.research.experimentplatform.exception.ConflictException;
import com.research.experimentplatform.exception.ForbiddenException;
import com.research.experimentplatform.exception.ResourceNotFoundException;
import com.research.experimentplatform.model.Enrollment;
import com.research.experimentplatform.model.EnrollmentStatus;
import com.research.experimentplatform.model.Experiment;
import com.research.experimentplatform.model.ExperimentStatus;
import com.research.experimentplatform.model.Group;
import com.research.experimentplatform.model.Invitation;
import com.research.experimentplatform.model.InvitationStatus;
import com.research.experimentplatform.model.Participant;
import com.research.experimentplatform.model.User;
import com.research.experimentplatform.repository.EnrollmentRepository;
import com.research.experimentplatform.repository.ExperimentRepository;
import com.research.experimentplatform.repository.GroupRepository;
import com.research.experimentplatform.repository.InvitationRepository;
import com.research.experimentplatform.repository.ParticipantRepository;
import com.research.experimentplatform.repository.UserRepository;
import com.research.experimentplatform.security.OwnershipChecker;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

@Service
public class InvitationService {

    private final InvitationRepository invitationRepository;
    private final ExperimentRepository experimentRepository;
    private final GroupRepository groupRepository;
    private final UserRepository userRepository;
    private final ParticipantRepository participantRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final EnrollmentService enrollmentService;
    private final OwnershipChecker ownershipChecker;

    public InvitationService(InvitationRepository invitationRepository,
                             ExperimentRepository experimentRepository,
                             GroupRepository groupRepository,
                             UserRepository userRepository,
                             ParticipantRepository participantRepository,
                             EnrollmentRepository enrollmentRepository,
                             EnrollmentService enrollmentService,
                             OwnershipChecker ownershipChecker) {
        this.invitationRepository = invitationRepository;
        this.experimentRepository = experimentRepository;
        this.groupRepository = groupRepository;
        this.userRepository = userRepository;
        this.participantRepository = participantRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.enrollmentService = enrollmentService;
        this.ownershipChecker = ownershipChecker;
    }

    @Transactional
    public InvitationDTO createInvitation(Long experimentId, CreateInvitationRequest request, String supabaseId) {
        Experiment experiment = experimentRepository.findById(experimentId)
                .orElseThrow(() -> new ResourceNotFoundException("Experiment not found"));

        if (!ownershipChecker.canModify(experiment, supabaseId)) {
            throw new ForbiddenException("You do not have permission to invite participants to this experiment");
        }

        if (experiment.getStatus() == ExperimentStatus.FINISHED) {
            throw new BadRequestException("Cannot invite participants to a finished experiment");
        }

        // Evitamos invitaciones duplicadas pendientes al mismo email
        if (invitationRepository.existsByExperimentIdAndInvitedEmailAndStatus(
                experimentId, request.email(), InvitationStatus.PENDING)) {
            throw new ConflictException("There is already a pending invitation for this email");
        }

        Group group = null;
        if (request.groupId() != null) {
            group = groupRepository.findById(request.groupId())
                    .orElseThrow(() -> new ResourceNotFoundException("Group not found"));
            if (!group.getExperiment().getId().equals(experimentId)) {
                throw new BadRequestException("Group does not belong to this experiment");
            }
        }

        // Las invitaciones expiran en 7 días por defecto
        Invitation invitation = new Invitation(experiment, request.email(), group,
                LocalDateTime.now().plusDays(7));
        Invitation saved = invitationRepository.save(invitation);
        return toDTO(saved);
    }

    public List<InvitationDTO> getInvitationsByExperiment(Long experimentId, String supabaseId) {
        Experiment experiment = experimentRepository.findById(experimentId)
                .orElseThrow(() -> new ResourceNotFoundException("Experiment not found"));

        if (!ownershipChecker.canModify(experiment, supabaseId)) {
            throw new ForbiddenException("You do not have permission to view these invitations");
        }

        return invitationRepository.findByExperimentId(experimentId).stream()
                .map(this::toDTO)
                .toList();
    }

    public InvitationDTO getInvitationByToken(String token) {
        Invitation invitation = invitationRepository.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Invitation not found"));
        return toDTO(invitation);
    }

    @Transactional
    public EnrollmentDTO acceptInvitation(String token, AcceptInvitationRequest request, String supabaseId) {
        Invitation invitation = invitationRepository.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Invitation not found"));

        validateInvitationUsable(invitation);

        if (!request.consentAgreed()) {
            throw new ForbiddenException("You must agree to the consent form to participate");
        }

        // Verificamos que el usuario autenticado sea el invitado
        User user = userRepository.findBySupabaseId(supabaseId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!user.getEmail().equalsIgnoreCase(invitation.getInvitedEmail())) {
            throw new ForbiddenException("This invitation was not sent to your email address");
        }

        Participant participant = participantRepository.findByUserId(user.getId())
                .orElseGet(() -> participantRepository.save(new Participant(user)));

        Experiment experiment = invitation.getExperiment();

        if (experiment.getStatus() == ExperimentStatus.FINISHED) {
            throw new BadRequestException("Experiment is already finished");
        }

        if (enrollmentRepository.existsByParticipantIdAndExperimentId(participant.getId(), experiment.getId())) {
            throw new ConflictException("You are already enrolled in this experiment");
        }

        // Si el experimento aún no ha empezado, la inscripción queda en PENDING
        // y se activará automáticamente cuando el investigador active el experimento.
        // Si ya está activo, la inscripción es ACTIVE directamente.
        EnrollmentStatus enrollmentStatus = experiment.getStatus() == ExperimentStatus.ACTIVE
                ? EnrollmentStatus.ACTIVE
                : EnrollmentStatus.PENDING;

        Enrollment enrollment = new Enrollment(participant, experiment, enrollmentStatus);
        enrollment.setConsentSignedAt(LocalDateTime.now());

        List<Group> experimentGroups = groupRepository.findByExperimentId(experiment.getId());
        if (!experimentGroups.isEmpty()) {
            int randomIndex = new Random().nextInt(experimentGroups.size());
            enrollment.setGroup(experimentGroups.get(randomIndex));
        }

        enrollmentRepository.save(enrollment);

        invitation.setStatus(InvitationStatus.ACCEPTED);
        invitationRepository.save(invitation);

        return enrollmentService.toDTO(enrollment);
    }

    @Transactional
    public void declineInvitation(String token, String supabaseId) {
        Invitation invitation = invitationRepository.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Invitation not found"));

        validateInvitationUsable(invitation);

        User user = userRepository.findBySupabaseId(supabaseId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!user.getEmail().equalsIgnoreCase(invitation.getInvitedEmail())) {
            throw new ForbiddenException("This invitation was not sent to your email address");
        }

        invitation.setStatus(InvitationStatus.DECLINED);
        invitationRepository.save(invitation);
    }

    private void validateInvitationUsable(Invitation invitation) {
        if (invitation.getStatus() == InvitationStatus.ACCEPTED) {
            throw new ConflictException("This invitation has already been accepted");
        }
        if (invitation.getStatus() == InvitationStatus.DECLINED) {
            throw new BadRequestException("This invitation has been declined");
        }
        if (invitation.getStatus() == InvitationStatus.EXPIRED) {
            throw new BadRequestException("This invitation has expired");
        }
        if (invitation.getExpiresAt() != null && LocalDateTime.now().isAfter(invitation.getExpiresAt())) {
            invitation.setStatus(InvitationStatus.EXPIRED);
            invitationRepository.save(invitation);
            throw new BadRequestException("This invitation has expired");
        }
    }

    public InvitationDTO toDTO(Invitation invitation) {
        return new InvitationDTO(
                invitation.getId(),
                invitation.getExperiment().getId(),
                invitation.getExperiment().getTitle(),
                invitation.getExperiment().getConsentText(),
                invitation.getInvitedEmail(),
                invitation.getGroup() != null ? invitation.getGroup().getId() : null,
                invitation.getGroup() != null ? invitation.getGroup().getName() : null,
                invitation.getToken(),
                invitation.getStatus(),
                invitation.getCreatedAt(),
                invitation.getExpiresAt()
        );
    }
}
