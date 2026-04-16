package com.research.experimentplatform.repository;

import com.research.experimentplatform.model.Participant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ParticipantRepository extends JpaRepository<Participant, Long> {
    
    Optional<Participant> findByUserId(Long userId);
    
    boolean existsByUserId(Long userId);
}
