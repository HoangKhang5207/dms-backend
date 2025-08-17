package com.genifast.dms.controller;

import static com.genifast.dms.common.constant.MessageCode.CommonMessage.*;

import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.genifast.dms.common.exception.ResourceNotFoundException;
import com.genifast.dms.dto.BaseResponseDto;
import com.genifast.dms.dto.task.response.ProcessTResponse;
import com.genifast.dms.dto.workflow.WorkflowDTO;
import com.genifast.dms.dto.workflow.WorkflowEleDTO;
import com.genifast.dms.dto.workflow.request.StartWRequest;
import com.genifast.dms.dto.workflow.response.AssignedWResponse;
import com.genifast.dms.service.workflow.WorkflowService;

@Validated
@RestController
@RequestMapping("/api/v1/workflow")
@RequiredArgsConstructor
@Slf4j
public class WorkflowController extends BaseController {
    private final WorkflowService workflowService;

    @GetMapping("/organization/{organization_id}")
    public ResponseEntity<BaseResponseDto> getWorkflowList(
            @PathVariable("organization_id") Long organizationId) {
        List<WorkflowDTO> workflowDtos = workflowService.getWorkflowList(organizationId);
        return success(workflowDtos, SUCCESS_GET_DATA);
    }

    @PostMapping("/deploy")
    @Transactional
    public ResponseEntity<BaseResponseDto> deploy(@RequestBody WorkflowDTO workflowDto) {
        try {
            String deploymentId = workflowService.deployProcess(workflowDto);
            log.info("[deploy] Success, deploymentId={}", deploymentId);
            return success(deploymentId, SUCCESS_CREATE_DATA);
        } catch (IllegalArgumentException e) {
            log.error("[deploy] Business error: {}", e.getMessage(), e);
            return badRequest(e.getMessage());
        } catch (IOException e) {
            log.error("[deploy] IO error: {}", e.getMessage(), e);
            return internalServerError("Error reading file: " + e.getMessage());
        } catch (Exception e) {
            log.error("[deploy] System error: {}", e.getMessage(), e);
            return internalServerError("Error during deployment: " + e.getMessage());
        }
    }

    @PostMapping("/assign_dept")
    @Transactional
    public ResponseEntity<BaseResponseDto> assignDept(@RequestBody WorkflowDTO workflowDto) {
        Long workflowId = workflowDto.getId();
        List<Long> departmentIds = workflowDto.getDepartmentIds();

        log.info("[assignDept] Input: workflowId ={}, departmentIds={}", workflowId, departmentIds);
        try {
            workflowService.assignDept(workflowId, departmentIds);
            log.info("[assignDept] Success");
            return success(SUCCESS_CREATE_DATA);
        } catch (ResourceNotFoundException e) {
            log.error("[assignDept] Not found: {}", e.getMessage(), e);
            return notFound(e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("[assignDept] Business error: {}", e.getMessage(), e);
            return badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("[assignDept] System error: {}", e.getMessage(), e);
            return internalServerError("Error starting workflow: " + e.getMessage());
        }
    }

    @PostMapping("/assign_ele")
    @Transactional
    public ResponseEntity<BaseResponseDto> assignEle(@RequestBody WorkflowDTO workflowDto) {
        Long workflowId = workflowDto.getId();
        WorkflowEleDTO workflowEleDto = workflowDto.getWorkflowEleDto();

        log.info("[assignEle] Input: workflowId ={}, request={}", workflowId, workflowEleDto);
        try {
            workflowService.assignEle(workflowId, workflowEleDto);
            log.info("[assignEle] Success");
            return success(SUCCESS_CREATE_DATA);
        } catch (ResourceNotFoundException e) {
            log.error("[assignEle] Not found: {}", e.getMessage(), e);
            return notFound(e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("[assignEle] Business error: {}", e.getMessage(), e);
            return badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("[assignEle] System error: {}", e.getMessage(), e);
            return internalServerError("Error starting workflow: " + e.getMessage());
        }
    }

    @GetMapping("/assigned")
    public ResponseEntity<BaseResponseDto> getAssignedWorkflows(
            @RequestParam(name = "department_id") long departmentId, String role) {
        try {
            List<WorkflowDTO> responses = workflowService.getAssignedWorkflows(departmentId, role);
            log.info("[getAssignedWorkflows] Success, result size={}", responses.size());
            return success(responses, SUCCESS_GET_DATA);
        } catch (Exception e) {
            log.error("[getAssignedWorkflows] System error: {}", e.getMessage(), e);
            return internalServerError("Error fetching assigned workflows: " + e.getMessage());
        }
    }

    @GetMapping("/assigned/{workflow_id}")
    public ResponseEntity<BaseResponseDto> getAssignedWorkflow(
            @PathVariable("workflow_id") Long workflowId) {
        log.info("[getAssignedWorkflow] Input: workflowId={}", workflowId);
        try {
            AssignedWResponse response = workflowService.getAssignedWorkflow(workflowId);
            if (response == null) {
                log.warn("[getAssignedWorkflow] Not found: workflowId={}", workflowId);
                throw new ResourceNotFoundException("Workflow not found with id: " + workflowId);
            }
            log.info("[getAssignedWorkflow] Success");
            return success(response, SUCCESS_GET_DATA);
        } catch (ResourceNotFoundException e) {
            log.error("[getAssignedWorkflow] Not found: {}", e.getMessage(), e);
            return notFound(e.getMessage());
        } catch (Exception e) {
            log.error("[getAssignedWorkflow] System error: {}", e.getMessage(), e);
            return internalServerError("Error fetching assigned workflow: " + e.getMessage());
        }
    }

    @PostMapping("/start")
    @Transactional
    public ResponseEntity<BaseResponseDto> startProcess(
            @Valid @RequestBody StartWRequest requestDto) {
        log.info("[startTask] Input: {}", requestDto);
        try {
            ProcessTResponse response = workflowService.startProcess(requestDto);
            log.info("[startTask] Success");
            return success(response, SUCCESS_CREATE_DATA);
        } catch (ResourceNotFoundException e) {
            log.error("[startTask] Not found: {}", e.getMessage(), e);
            return notFound(e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("[startTask] Business error: {}", e.getMessage(), e);
            return badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("[startTask] System error: {}", e.getMessage(), e);
            return internalServerError("Error starting workflow: " + e.getMessage());
        }
    }
}
