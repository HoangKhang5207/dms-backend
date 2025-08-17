package com.genifast.dms.repository.workflow;

import java.util.List;
import org.springframework.stereotype.Repository;

import com.genifast.dms.entity.workflow.WorkflowSteps;
import com.genifast.dms.repository.BaseRepository;

@Repository
public interface WorkflowStepsRepository extends BaseRepository<WorkflowSteps, Long> {
  List<WorkflowSteps> findByProcessKeyAndWorkflowIdAndTaskKey(String processKey, Long workflowId, String taskKey);

  List<WorkflowSteps> findByProcessKeyAndWorkflowIdAndTaskKeyIn(String processKey, Long workflowId,
      List<String> taskKey);

  List<WorkflowSteps> findByWorkflowIdAndTaskKeyIn(Long workflowId, List<String> nextKeys);

  WorkflowSteps findByTaskKeyAndCondition(String taskKey, String condition);
}
