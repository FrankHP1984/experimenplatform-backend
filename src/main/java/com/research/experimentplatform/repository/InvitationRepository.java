package com.research.experimentplatform.repository;

import com.research.experimentplatform.model.Invitation;
import com.research.experimentplatform.model.InvitationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InvitationRepository extends JpaRepository<Invitation, Long> {

    Optional<Invitation> findByToken(String token);

    List<Invitation> findByExperimentId(Long experimentId);

    boolean existsByExperimentIdAndInvitedEmailAndStatus(Long experimentId, String email, InvitationStatus status);
}
