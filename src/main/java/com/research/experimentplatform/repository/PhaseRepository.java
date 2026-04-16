package com.research.experimentplatform.repository;

import com.research.experimentplatform.model.Phase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PhaseRepository extends JpaRepository<Phase, Long> {
    
    List<Phase> findByExperimentId(Long experimentId);
    
    List<Phase> findByExperimentIdOrderByPhaseOrderAsc(Long experimentId);
}
