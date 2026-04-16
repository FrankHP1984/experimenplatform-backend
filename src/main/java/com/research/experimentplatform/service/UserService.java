package com.research.experimentplatform.service;

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
    public SyncResult syncUser(String supabaseId, String email, UserRole role) {
        return userRepository.findBySupabaseId(supabaseId)
                .map(existing -> {
                    // El rol no se actualiza en syncs posteriores — es una operación de admin
                    existing.setEmail(email);
                    return new SyncResult(convertToDTO(userRepository.save(existing)), false);
                })
                .orElseGet(() -> {
                    User newUser = new User(supabaseId, email, role != null ? role : UserRole.PARTICIPANT);
                    return new SyncResult(convertToDTO(userRepository.save(newUser)), true);
                });
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

    public UserDTO convertToDTO(User user) {
        return new UserDTO(user.getId(), user.getEmail(), user.getRole());
    }
}
