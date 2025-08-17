package com.genifast.dms.repository.workflow;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.genifast.dms.entity.workflow.WorkflowDept;
import com.genifast.dms.repository.BaseRepository;

@Repository
public interface WorkflowOrgRepository extends BaseRepository<WorkflowDept, Long> {
    List<WorkflowDept> findByWorkflowId(Long workflowId);
}
