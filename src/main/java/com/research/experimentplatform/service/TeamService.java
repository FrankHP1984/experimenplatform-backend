package com.research.experimentplatform.service;

import com.research.experimentplatform.dto.AddTeamMemberRequest;
import com.research.experimentplatform.dto.CreateTeamRequest;
import com.research.experimentplatform.dto.TeamDTO;
import com.research.experimentplatform.dto.TeamMemberDTO;
import com.research.experimentplatform.exception.BadRequestException;
import com.research.experimentplatform.exception.ConflictException;
import com.research.experimentplatform.exception.ForbiddenException;
import com.research.experimentplatform.exception.ResourceNotFoundException;
import com.research.experimentplatform.model.ResearchTeam;
import com.research.experimentplatform.model.TeamMembership;
import com.research.experimentplatform.model.TeamRole;
import com.research.experimentplatform.model.User;
import com.research.experimentplatform.repository.ResearchTeamRepository;
import com.research.experimentplatform.repository.TeamMembershipRepository;
import com.research.experimentplatform.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TeamService {

    private final ResearchTeamRepository teamRepository;
    private final TeamMembershipRepository membershipRepository;
    private final UserRepository userRepository;

    public TeamService(ResearchTeamRepository teamRepository,
                       TeamMembershipRepository membershipRepository,
                       UserRepository userRepository) {
        this.teamRepository = teamRepository;
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public TeamDTO createTeam(CreateTeamRequest request, String supabaseId) {
        User creator = userRepository.findBySupabaseId(supabaseId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        ResearchTeam team = new ResearchTeam(request.name(), request.description());
        ResearchTeam saved = teamRepository.save(team);

        // El creador queda como OWNER del equipo
        TeamMembership membership = new TeamMembership(creator, saved, TeamRole.OWNER);
        membershipRepository.save(membership);

        return toDTO(saved);
    }

    public TeamDTO getTeam(Long teamId, String supabaseId) {
        ResearchTeam team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team not found"));
        validateIsMember(supabaseId, teamId);
        return toDTO(team);
    }

    public List<TeamDTO> getMyTeams(String supabaseId) {
        User user = userRepository.findBySupabaseId(supabaseId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return membershipRepository.findByUserId(user.getId()).stream()
                .map(m -> toDTO(m.getTeam()))
                .toList();
    }

    public List<TeamMemberDTO> getTeamMembers(Long teamId, String supabaseId) {
        validateIsMember(supabaseId, teamId);
        return membershipRepository.findByTeamId(teamId).stream()
                .map(this::toMemberDTO)
                .toList();
    }

    @Transactional
    public TeamMemberDTO addMember(Long teamId, AddTeamMemberRequest request, String supabaseId) {
        validateIsOwner(supabaseId, teamId);

        if (membershipRepository.existsByUserIdAndTeamId(request.userId(), teamId)) {
            throw new ConflictException("User is already a member of this team");
        }

        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        ResearchTeam team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team not found"));

        TeamMembership membership = new TeamMembership(user, team, request.role());
        return toMemberDTO(membershipRepository.save(membership));
    }

    @Transactional
    public void removeMember(Long teamId, Long userId, String supabaseId) {
        validateIsOwner(supabaseId, teamId);

        TeamMembership membership = membershipRepository.findByUserIdAndTeamId(userId, teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found"));

        // No se puede eliminar al último OWNER del equipo
        if (membership.getRole() == TeamRole.OWNER) {
            long ownerCount = membershipRepository.findByTeamId(teamId).stream()
                    .filter(m -> m.getRole() == TeamRole.OWNER).count();
            if (ownerCount <= 1) {
                throw new BadRequestException("Cannot remove the last owner of the team");
            }
        }

        membershipRepository.delete(membership);
    }

    private void validateIsMember(String supabaseId, Long teamId) {
        if (!membershipRepository.existsByUser_SupabaseIdAndTeam_Id(supabaseId, teamId)) {
            throw new ForbiddenException("You are not a member of this team");
        }
    }

    private void validateIsOwner(String supabaseId, Long teamId) {
        User user = userRepository.findBySupabaseId(supabaseId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (!membershipRepository.existsByUserIdAndTeamIdAndRole(user.getId(), teamId, TeamRole.OWNER)) {
            throw new ForbiddenException("Only the team owner can perform this action");
        }
    }

    public TeamDTO toDTO(ResearchTeam team) {
        return new TeamDTO(team.getId(), team.getName(), team.getDescription(), team.getCreatedAt());
    }

    public TeamMemberDTO toMemberDTO(TeamMembership membership) {
        return new TeamMemberDTO(
                membership.getUser().getId(),
                membership.getUser().getEmail(),
                membership.getRole(),
                membership.getJoinedAt()
        );
    }
}
