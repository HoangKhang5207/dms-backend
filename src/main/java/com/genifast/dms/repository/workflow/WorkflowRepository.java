package com.genifast.dms.repository.workflow;

import org.springframework.data.jpa.repository.JpaRepository;

import com.genifast.dms.entity.workflow.Workflow;

public interface WorkflowRepository extends JpaRepository<Workflow, Long> {
}
