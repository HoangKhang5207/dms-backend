package com.genifast.dms.repository;

import com.genifast.dms.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    Optional<Project> findByNameAndOrganizationId(String name, Long organizationId);

    List<Project> findAllByStatusAndEndDateBefore(Integer status, Instant now);
}