package com.research.experimentplatform.service;

import com.research.experimentplatform.dto.CreateGroupRequest;
import com.research.experimentplatform.dto.GroupDTO;
import com.research.experimentplatform.model.Experiment;
import com.research.experimentplatform.model.Group;
import com.research.experimentplatform.exception.ForbiddenException;
import com.research.experimentplatform.exception.ResourceNotFoundException;
import com.research.experimentplatform.repository.ExperimentRepository;
import com.research.experimentplatform.repository.GroupRepository;
import com.research.experimentplatform.security.OwnershipChecker;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class GroupService {

    private final GroupRepository groupRepository;
    private final ExperimentRepository experimentRepository;
    private final OwnershipChecker ownershipChecker;

    public GroupService(GroupRepository groupRepository, ExperimentRepository experimentRepository,
                        OwnershipChecker ownershipChecker) {
        this.groupRepository = groupRepository;
        this.experimentRepository = experimentRepository;
        this.ownershipChecker = ownershipChecker;
    }

    @Transactional
    public GroupDTO createGroup(Long experimentId, CreateGroupRequest request, String supabaseId) {
        Experiment experiment = experimentRepository.findById(experimentId)
                .orElseThrow(() -> new ResourceNotFoundException("Experiment not found"));

        if (!ownershipChecker.canModify(experiment, supabaseId)) {
            throw new ForbiddenException("You do not have permission to modify this experiment");
        }

        Group group = new Group();
        group.setName(request.getName());
        group.setDescription(request.getDescription());
        group.setExperiment(experiment);

        Group savedGroup = groupRepository.save(group);
        return convertToDTO(savedGroup);
    }

    public List<GroupDTO> getGroupsByExperiment(Long experimentId) {
        return groupRepository.findByExperimentId(experimentId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public GroupDTO getGroup(Long id) {
        Group group = groupRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found"));
        return convertToDTO(group);
    }

    @Transactional
    public void deleteGroup(Long id, String supabaseId) {
        Group group = groupRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found"));
        if (!ownershipChecker.canModify(group.getExperiment(), supabaseId)) {
            throw new ForbiddenException("You do not have permission to modify this experiment");
        }
        groupRepository.delete(group);
    }

    public GroupDTO convertToDTO(Group group) {
        return new GroupDTO(
                group.getId(),
                group.getName(),
                group.getDescription(),
                group.getExperiment().getId()
        );
    }
}
