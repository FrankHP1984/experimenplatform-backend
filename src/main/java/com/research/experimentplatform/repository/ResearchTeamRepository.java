package com.research.experimentplatform.repository;

import com.research.experimentplatform.model.ResearchTeam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ResearchTeamRepository extends JpaRepository<ResearchTeam, Long> {
}
