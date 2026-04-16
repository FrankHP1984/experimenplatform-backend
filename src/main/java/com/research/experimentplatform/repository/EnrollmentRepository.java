package com.research.experimentplatform.repository;

import com.research.experimentplatform.model.Enrollment;
import com.research.experimentplatform.model.EnrollmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
    
    List<Enrollment> findByParticipantId(Long participantId);

    Page<Enrollment> findByParticipantId(Long participantId, Pageable pageable);
    
    List<Enrollment> findByExperimentId(Long experimentId);

    Page<Enrollment> findByExperimentId(Long experimentId, Pageable pageable);
    
    List<Enrollment> findByExperimentIdAndStatus(Long experimentId, EnrollmentStatus status);
    
    Optional<Enrollment> findByParticipantIdAndExperimentId(Long participantId, Long experimentId);
    
    boolean existsByParticipantIdAndExperimentId(Long participantId, Long experimentId);
    
    long countByExperimentIdAndStatus(Long experimentId, EnrollmentStatus status);

    long countByStatus(EnrollmentStatus status);
}
