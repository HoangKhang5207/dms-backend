package com.genifast.dms.service.workflow;

import java.util.List;

import com.genifast.dms.dto.workflow.WorkflowStepsDTO;
import com.genifast.dms.entity.workflow.WorkflowSteps;

public interface WorkflowStepsService {
  List<WorkflowStepsDTO> getWorkflowStepsDtos(
      List<WorkflowStepsDTO> currentWStepDtos, List<WorkflowSteps> nextWSteps);
}