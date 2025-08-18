package com.genifast.dms.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.genifast.dms.dto.request.workflow.WorkflowAssignDeptRequest;
import com.genifast.dms.dto.request.workflow.WorkflowAssignEleRequest;
import com.genifast.dms.dto.request.workflow.WorkflowDeployRequest;
import com.genifast.dms.dto.request.workflow.WorkflowStartRequest;
import com.genifast.dms.dto.response.workflow.DeployWorkflowResponse;
import com.genifast.dms.dto.response.workflow.ProcessTResponse;
import com.genifast.dms.service.workflow.WorkflowService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/workflows")
@RequiredArgsConstructor
@Validated
public class WorkflowController {

    private final WorkflowService workflowService;

    // POST /api/v1/workflows/deploy
    @PostMapping("/deploy")
    public ResponseEntity<DeployWorkflowResponse> deploy(@Valid @RequestBody WorkflowDeployRequest req) {
        DeployWorkflowResponse resp = workflowService.deploy(req);
        return new ResponseEntity<>(resp, HttpStatus.OK);
    }

    // POST /api/v1/workflows/assign_dept
    @PostMapping("/assign_dept")
    public ResponseEntity<Void> assignDepartments(@Valid @RequestBody WorkflowAssignDeptRequest req) {
        workflowService.assignDepartments(req);
        return ResponseEntity.ok().build();
    }

    // POST /api/v1/workflows/assign_ele
    @PostMapping("/assign_ele")
    public ResponseEntity<Void> assignElements(@Valid @RequestBody WorkflowAssignEleRequest req) {
        workflowService.assignElements(req);
        return ResponseEntity.ok().build();
    }

    // POST /api/v1/workflows/start
    @PostMapping("/start")
    public ResponseEntity<ProcessTResponse> start(@Valid @RequestBody WorkflowStartRequest req) {
        ProcessTResponse resp = workflowService.start(req);
        return ResponseEntity.ok(resp);
    }
}
