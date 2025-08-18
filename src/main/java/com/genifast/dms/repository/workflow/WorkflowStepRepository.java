package com.genifast.dms.repository.workflow;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.genifast.dms.entity.workflow.WorkflowStep;

public interface WorkflowStepRepository extends JpaRepository<WorkflowStep, Long> {
    List<WorkflowStep> findByWorkflow_IdOrderByStepOrderAsc(Long workflowId);
    List<WorkflowStep> findByProcessKey(String processKey);
}
