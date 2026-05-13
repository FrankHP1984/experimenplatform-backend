package com.research.experimentplatform.security;

import com.research.experimentplatform.exception.ForbiddenException;
import com.research.experimentplatform.exception.ResourceNotFoundException;
import com.research.experimentplatform.model.Experiment;
import com.research.experimentplatform.repository.ExperimentRepository;
import org.springframework.stereotype.Component;

@Component
public class OwnershipChecker {

    private final ExperimentRepository experimentRepository;

    public OwnershipChecker(ExperimentRepository experimentRepository) {
        this.experimentRepository = experimentRepository;
    }

    public boolean canModify(Experiment experiment, String supabaseId) {
        return experiment.getOwner().getSupabaseId().equals(supabaseId);
    }

    public void checkExperimentOwnership(Long experimentId, String supabaseId) {
        Experiment experiment = experimentRepository.findById(experimentId)
                .orElseThrow(() -> new ResourceNotFoundException("Experiment not found"));
        if (!canModify(experiment, supabaseId)) {
            throw new ForbiddenException("You don't have permission to modify this experiment");
        }
    }
}
