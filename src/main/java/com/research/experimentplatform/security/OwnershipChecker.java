package com.research.experimentplatform.security;

import com.research.experimentplatform.model.Experiment;
import com.research.experimentplatform.repository.TeamMembershipRepository;
import org.springframework.stereotype.Component;

@Component
public class OwnershipChecker {

    private final TeamMembershipRepository teamMembershipRepository;

    public OwnershipChecker(TeamMembershipRepository teamMembershipRepository) {
        this.teamMembershipRepository = teamMembershipRepository;
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
}
