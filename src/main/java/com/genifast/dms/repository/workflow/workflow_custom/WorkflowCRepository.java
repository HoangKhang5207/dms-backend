package com.genifast.dms.repository.workflow.workflow_custom;

import java.util.List;
import org.springframework.stereotype.Repository;

import com.genifast.dms.dto.workflow.AssignedWorkflow;
import com.genifast.dms.entity.workflow.Workflow;

@Repository
public interface WorkflowCRepository {
    List<Workflow> getAssignedWorkflows(String role, Long departmentId);

    List<AssignedWorkflow> getAssignedWorkflow(Long workflowId);
}
