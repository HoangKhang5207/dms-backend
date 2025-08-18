package com.genifast.dms.repository.workflow;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.genifast.dms.entity.workflow.WorkflowOrg;

public interface WorkflowOrgRepository extends JpaRepository<WorkflowOrg, Long> {
    List<WorkflowOrg> findByWorkflow_IdAndIsDeletedFalse(Long workflowId);
}
