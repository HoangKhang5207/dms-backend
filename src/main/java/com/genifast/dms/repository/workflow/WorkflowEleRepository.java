package com.genifast.dms.repository.workflow;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.genifast.dms.entity.workflow.WorkflowEle;

public interface WorkflowEleRepository extends JpaRepository<WorkflowEle, Long> {
    List<WorkflowEle> findByWorkflow_IdAndIsDeletedFalse(Long workflowId);
}
