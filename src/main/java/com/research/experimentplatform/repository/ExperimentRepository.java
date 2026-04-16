package com.research.experimentplatform.repository;

import com.research.experimentplatform.model.Experiment;
import com.research.experimentplatform.model.ExperimentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExperimentRepository extends JpaRepository<Experiment, Long> {

    List<Experiment> findByOwnerId(Long ownerId);

    Page<Experiment> findByOwnerId(Long ownerId, Pageable pageable);

    long countByStatus(ExperimentStatus status);
}
