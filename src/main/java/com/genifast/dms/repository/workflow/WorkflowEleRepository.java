package com.genifast.dms.repository.workflow;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.genifast.dms.entity.workflow.WorkflowEle;
import com.genifast.dms.repository.BaseRepository;

@Repository
public interface WorkflowEleRepository extends BaseRepository<WorkflowEle, Long> {
  Optional<WorkflowEle> findByWorkflowId(Long workflowId);
}
