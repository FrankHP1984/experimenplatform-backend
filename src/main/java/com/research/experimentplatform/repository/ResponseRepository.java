package com.research.experimentplatform.repository;

import com.research.experimentplatform.model.Response;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ResponseRepository extends JpaRepository<Response, Long> {
    
    List<Response> findByEnrollmentId(Long enrollmentId);
    
    List<Response> findByQuestionId(Long questionId);
    
    Optional<Response> findByEnrollmentIdAndQuestionId(Long enrollmentId, Long questionId);
    
    @Query("SELECT r FROM Response r WHERE r.enrollment.id = :enrollmentId AND r.question.phase.id = :phaseId")
    List<Response> findByEnrollmentIdAndPhaseId(@Param("enrollmentId") Long enrollmentId, @Param("phaseId") Long phaseId);
    
    @Query("SELECT r FROM Response r WHERE r.question.phase.experiment.id = :experimentId")
    List<Response> findByExperimentId(@Param("experimentId") Long experimentId);
    
    boolean existsByEnrollmentIdAndQuestionId(Long enrollmentId, Long questionId);
}
