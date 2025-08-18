package com.genifast.dms.service.workflow;

import com.genifast.dms.dto.request.workflow.WorkflowAssignDeptRequest;
import com.genifast.dms.dto.request.workflow.WorkflowAssignEleRequest;
import com.genifast.dms.dto.request.workflow.WorkflowDeployRequest;
import com.genifast.dms.dto.request.workflow.WorkflowStartRequest;
import com.genifast.dms.dto.response.workflow.DeployWorkflowResponse;
import com.genifast.dms.dto.response.workflow.ProcessTResponse;

public interface WorkflowService {

    DeployWorkflowResponse deploy(WorkflowDeployRequest req);

    void assignDepartments(WorkflowAssignDeptRequest req);

    void assignElements(WorkflowAssignEleRequest req);

    ProcessTResponse start(WorkflowStartRequest req);
}
