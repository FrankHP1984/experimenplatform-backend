package com.research.experimentplatform.security;

import com.research.experimentplatform.exception.ForbiddenException;
import com.research.experimentplatform.exception.ResourceNotFoundException;
import com.research.experimentplatform.model.Experiment;
import com.research.experimentplatform.repository.ExperimentRepository;
import com.research.experimentplatform.repository.TeamMembershipRepository;
import org.springframework.stereotype.Component;

@Component
public class OwnershipChecker {

    private final TeamMembershipRepository teamMembershipRepository;
    private final ExperimentRepository experimentRepository;

    public OwnershipChecker(TeamMembershipRepository teamMembershipRepository,
                           ExperimentRepository experimentRepository) {
        this.teamMembershipRepository = teamMembershipRepository;
        this.experimentRepository = experimentRepository;
    }

    /**
     * Devuelve true si el usuario es el dueño directo del experimento
     * o es miembro del equipo asignado al experimento.
     */
    public boolean canModify(Experiment experiment, String supabaseId) {
        if (experiment.getOwner().getSupabaseId().equals(supabaseId)) {
            return true;
        }
        if (experiment.getTeam() != null) {
            return teamMembershipRepository.existsByUser_SupabaseIdAndTeam_Id(
                    supabaseId, experiment.getTeam().getId());
        }
        return false;
    }

    /**
     * Devuelve true si el usuario es miembro de un equipo concreto.
     */
    public boolean isMemberOf(String supabaseId, Long teamId) {
        return teamMembershipRepository.existsByUser_SupabaseIdAndTeam_Id(supabaseId, teamId);
    }

    /**
     * Verifica que el usuario tiene permisos para modificar el experimento.
     * Lanza ForbiddenException si no tiene permisos.
     */
    public void checkExperimentOwnership(Long experimentId, String supabaseId) {
        Experiment experiment = experimentRepository.findById(experimentId)
                .orElseThrow(() -> new ResourceNotFoundException("Experiment not found"));
        
        if (!canModify(experiment, supabaseId)) {
            throw new ForbiddenException("You don't have permission to modify this experiment");
        }
    }
}
