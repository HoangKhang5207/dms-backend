package com.genifast.dms.service.workflow;

import java.io.IOException;
import java.util.List;

import com.genifast.dms.dto.task.response.ProcessTResponse;
import com.genifast.dms.dto.workflow.WorkflowDTO;
import com.genifast.dms.dto.workflow.WorkflowEleDTO;
import com.genifast.dms.dto.workflow.WorkflowStepsDTO;
import com.genifast.dms.dto.workflow.request.StartWRequest;
import com.genifast.dms.dto.workflow.response.AssignedWResponse;
import com.genifast.dms.entity.workflow.Workflow;
import com.genifast.dms.entity.workflow.WorkflowSteps;
import com.genifast.dms.service.BaseService;

public interface WorkflowService extends BaseService<Workflow, Long> {
  List<WorkflowDTO> getWorkflowList(Long organizationId);

  String deployProcess(WorkflowDTO workflowDto) throws IOException;

  void assignDept(Long workflowId, List<Long> departmentIds);

  void assignEle(Long workflowId, WorkflowEleDTO workflowEleDTO);

  ProcessTResponse startProcess(StartWRequest startWRequestDTO);

  List<WorkflowDTO> getAssignedWorkflows(Long departmentId, String role);

  AssignedWResponse getAssignedWorkflow(Long workflowId);

  public List<WorkflowStepsDTO> getWorkflowStepsDtos(
      List<WorkflowStepsDTO> currentWStepDtos, List<WorkflowSteps> nextWSteps);
}
