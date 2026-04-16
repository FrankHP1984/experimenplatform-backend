package com.research.experimentplatform.service;

import com.research.experimentplatform.dto.ParticipantDTO;
import com.research.experimentplatform.dto.UpdateParticipantRequest;
import com.research.experimentplatform.model.Participant;
import com.research.experimentplatform.model.User;
import com.research.experimentplatform.exception.ConflictException;
import com.research.experimentplatform.exception.ForbiddenException;
import com.research.experimentplatform.exception.ResourceNotFoundException;
import com.research.experimentplatform.repository.ParticipantRepository;
import com.research.experimentplatform.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ParticipantService {

    private final ParticipantRepository participantRepository;
    private final UserRepository userRepository;

    public ParticipantService(ParticipantRepository participantRepository,
                             UserRepository userRepository) {
        this.participantRepository = participantRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public ParticipantDTO createParticipant(String supabaseId) {
        User user = userRepository.findBySupabaseId(supabaseId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (participantRepository.existsByUserId(user.getId())) {
            throw new ConflictException("Participant already exists for this user");
        }

        Participant participant = new Participant(user);
        return convertToDTO(participantRepository.save(participant));
    }

    public ParticipantDTO getParticipantByUserId(String supabaseId) {
        User user = userRepository.findBySupabaseId(supabaseId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Participant participant = participantRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Participant not found"));
        return convertToDTO(participant);
    }

    public ParticipantDTO getParticipant(Long id) {
        Participant participant = participantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Participant not found"));
        return convertToDTO(participant);
    }

    @Transactional
    public ParticipantDTO updateParticipant(Long id, UpdateParticipantRequest request, String supabaseId) {
        Participant participant = participantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Participant not found"));

        if (!participant.getUser().getSupabaseId().equals(supabaseId)) {
            throw new ForbiddenException("You can only update your own profile");
        }

        if (request.getBio() != null) {
            participant.setBio(request.getBio());
        }

        Participant updatedParticipant = participantRepository.save(participant);
        return convertToDTO(updatedParticipant);
    }

    public ParticipantDTO convertToDTO(Participant participant) {
        return new ParticipantDTO(
                participant.getId(),
                participant.getUser().getId(),
                participant.getUser().getEmail(),
                participant.getBio(),
                participant.getCreatedAt()
        );
    }
}
