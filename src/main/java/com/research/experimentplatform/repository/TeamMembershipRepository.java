package com.research.experimentplatform.repository;

import com.research.experimentplatform.model.TeamMembership;
import com.research.experimentplatform.model.TeamRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeamMembershipRepository extends JpaRepository<TeamMembership, Long> {

    List<TeamMembership> findByTeamId(Long teamId);

    List<TeamMembership> findByUserId(Long userId);

    Optional<TeamMembership> findByUserIdAndTeamId(Long userId, Long teamId);

    boolean existsByUserIdAndTeamId(Long userId, Long teamId);

    boolean existsByUser_SupabaseIdAndTeam_Id(String supabaseId, Long teamId);

    boolean existsByUserIdAndTeamIdAndRole(Long userId, Long teamId, TeamRole role);
}
