package com.research.experimentplatform.repository;

import com.research.experimentplatform.model.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, Long> {

    boolean existsByName(String name);
}
