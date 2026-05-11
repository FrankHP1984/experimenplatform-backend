package com.research.experimentplatform.service;

import com.research.experimentplatform.dto.UpdateUserRequest;
import com.research.experimentplatform.dto.UserDTO;
import com.research.experimentplatform.exception.ResourceNotFoundException;
import com.research.experimentplatform.model.User;
import com.research.experimentplatform.model.UserRole;
import com.research.experimentplatform.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserDTO getUserBySupabaseId(String supabaseId) {
        User user = userRepository.findBySupabaseId(supabaseId)
                .orElseThrow(() -> new com.research.experimentplatform.exception.ResourceNotFoundException("User not found"));
        return convertToDTO(user);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    public record SyncResult(UserDTO user, boolean created) {}

    @Transactional
    public SyncResult syncUser(String supabaseId, String email, UserRole role, String firstName, String lastName) {
        Optional<User> bySupabaseId = userRepository.findBySupabaseId(supabaseId);
        if (bySupabaseId.isPresent()) {
            User existing = bySupabaseId.get();
            existing.setEmail(email);
            if (role != null) existing.setRole(role);
            if (firstName != null && existing.getFirstName() == null) existing.setFirstName(firstName);
            if (lastName  != null && existing.getLastName()  == null) existing.setLastName(lastName);
            return new SyncResult(convertToDTO(userRepository.save(existing)), false);
        }

        // El mismo email puede existir ya (registro previo parcial): enlazar el supabaseId en vez de duplicar
        Optional<User> byEmail = userRepository.findByEmail(email);
        if (byEmail.isPresent()) {
            User existing = byEmail.get();
            existing.setSupabaseId(supabaseId);
            if (role != null) existing.setRole(role);
            if (firstName != null && existing.getFirstName() == null) existing.setFirstName(firstName);
            if (lastName  != null && existing.getLastName()  == null) existing.setLastName(lastName);
            return new SyncResult(convertToDTO(userRepository.save(existing)), false);
        }

        User newUser = new User(supabaseId, email, role != null ? role : UserRole.PARTICIPANT);
        newUser.setFirstName(firstName);
        newUser.setLastName(lastName);
        return new SyncResult(convertToDTO(userRepository.save(newUser)), true);
    }

    public Page<UserDTO> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(this::convertToDTO);
    }

    @Transactional
    public UserDTO updateUserRole(Long id, UserRole newRole) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setRole(newRole);
        return convertToDTO(userRepository.save(user));
    }

    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User not found");
        }
        userRepository.deleteById(id);
    }

    @Transactional
    public UserDTO updateMe(String supabaseId, UpdateUserRequest req) {
        User user = userRepository.findBySupabaseId(supabaseId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (req.firstName() != null) { user.setFirstName(req.firstName()); }
        if (req.name() != null && req.firstName() == null) { user.setFirstName(req.name()); }
        if (req.lastName() != null) { user.setLastName(req.lastName()); }
        if (req.institution() != null) { user.setInstitution(req.institution()); }
        if (req.department() != null) { user.setDepartment(req.department()); }
        if (req.position() != null) { user.setPosition(req.position()); }
        if (req.roleTitle() != null && req.position() == null) { user.setPosition(req.roleTitle()); }
        if (req.bio() != null) { user.setBio(req.bio()); }
        if (req.orcidId() != null) { user.setOrcidId(req.orcidId()); }
        if (req.language() != null) { user.setLanguage(req.language()); }
        if (req.timezone() != null) { user.setTimezone(req.timezone()); }
        if (req.researchArea() != null) { user.setResearchArea(req.researchArea()); }
        if (req.preferredDesign() != null) { user.setPreferredDesign(req.preferredDesign()); }

        return convertToDTO(userRepository.save(user));
    }

    public UserDTO convertToDTO(User user) {
        return new UserDTO(
            user.getId(),
            user.getEmail(),
            user.getRole(),
            user.getFirstName(),
            user.getLastName(),
            user.getInstitution(),
            user.getDepartment(),
            user.getPosition(),
            user.getBio(),
            user.getOrcidId(),
            user.getLanguage(),
            user.getTimezone(),
            user.getResearchArea(),
            user.getPreferredDesign()
        );
    }
}
