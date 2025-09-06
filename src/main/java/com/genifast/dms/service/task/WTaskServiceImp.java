package com.genifast.dms.service.task;

import java.util.*;
import lombok.AllArgsConstructor;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.springframework.stereotype.Service;

import com.genifast.dms.common.exception.ResourceNotFoundException;
import com.genifast.dms.dto.task.request.ProcessTRequest;
import com.genifast.dms.dto.task.response.ProcessTResponse;
import com.genifast.dms.dto.task.response.TaskInfoResponse;
import com.genifast.dms.dto.workflow.AssignedWorkflow;
import com.genifast.dms.dto.workflow.WorkflowStepsDTO;
import com.genifast.dms.entity.bpmn.BpmnUpload;
import com.genifast.dms.entity.workflow.WorkflowEle;
import com.genifast.dms.entity.workflow.WorkflowSteps;
import com.genifast.dms.mapper.bpmn.BpmnUploadMapper;
import com.genifast.dms.mapper.workflow.WorkflowEleMapper;
import com.genifast.dms.mapper.workflow.WorkflowStepsMapper;
import com.genifast.dms.repository.workflow.WorkflowRepository;
import com.genifast.dms.repository.workflow.WorkflowStepsRepository;
import com.genifast.dms.service.bpmn.BpmnService;
import com.genifast.dms.service.workflow.WorkflowStepsService;

@Service
@AllArgsConstructor
public class WTaskServiceImp implements WTaskService {
  private final TaskService taskService;
  private final RuntimeService runtimeService;

  private final BpmnService bpmnService;
  private final WorkflowStepsService workflowStepsService;

  private final BpmnUploadMapper bpmnUploadMapper;
  private final WorkflowEleMapper workflowEleMapper;
  private final WorkflowStepsMapper workflowStepsMapper;

  private final WorkflowRepository workflowRepository;
  private final WorkflowStepsRepository workflowStepsRepository;

  @Override
  public TaskInfoResponse getTaskInfo(Long documentId) {
    try {
      TaskInfoResponse response = new TaskInfoResponse();
      ProcessInstance processInstance = getProcess(documentId);
      if (processInstance == null) {
        throw new ResourceNotFoundException(
            "Process instance not found for document id: " + documentId);
      }
      Map<String, Object> variables = runtimeService.getVariables(processInstance.getId());
      response.setBpmnUpload_id((Long) variables.get("bpmnUpload_id"));

      Long workflowId = (Long) variables.get("workflowId");
      List<AssignedWorkflow> assignedWorkflows = workflowRepository.getAssignedWorkflow(workflowId);
      if (assignedWorkflows == null) {
        // log.warn("[getAssignedWorkflow] Not found: workflowId={}", workflowId);
        throw new ResourceNotFoundException("Workflow not found with id: " + workflowId);
      }
      AssignedWorkflow assignedWorkflow = assignedWorkflows.get(0);

      WorkflowEle workflowEle = new WorkflowEle(
          assignedWorkflow.getCategoryIds(),
          assignedWorkflow.getUrgency(),
          assignedWorkflow.getSecurity());
      response.setWorkflowEleDto(workflowEleMapper.toDto(workflowEle));
      response.setBpmnUploadDto(bpmnUploadMapper.toDto(assignedWorkflow.getBpmnUpload()));

      Task task = getTask(processInstance.getId());
      if (task == null) {
        throw new ResourceNotFoundException(
            "Task not found for process instance id: " + processInstance.getId());
      }
      BpmnUpload bpmnUpload = bpmnService.getBpmnUpload((Long) variables.get("bpmnUpload_id"));
      List<WorkflowSteps> currentWSteps = workflowStepsRepository.findByProcessKeyAndWorkflowIdAndTaskKey(
          bpmnUpload.getProcessKey(), workflowId, task.getTaskDefinitionKey());
      List<String> nextKeys = currentWSteps.stream()
          .map(step -> step.getNextKey() != null ? step.getNextKey() : step.getRejectKey())
          .filter(Objects::nonNull)
          .toList();
      List<WorkflowSteps> nextWSteps = workflowStepsRepository.findByProcessKeyAndWorkflowIdAndTaskKeyIn(
          bpmnUpload.getProcessKey(), workflowId, nextKeys);
      if (nextWSteps == null) {
        throw new ResourceNotFoundException(
            "Workflow steps not found for process definition: "
                + processInstance.getProcessDefinitionId()
                + " and task key: "
                + task.getTaskDefinitionKey());
      }
      List<WorkflowStepsDTO> workflowStepDtos = workflowStepsMapper.toDtos(currentWSteps);
      List<WorkflowStepsDTO> workflowStepsDtos = workflowStepsService.getWorkflowStepsDtos(workflowStepDtos,
          nextWSteps);
      response.setWorkflowStepDtos(workflowStepsDtos);
      return response;
    } catch (ResourceNotFoundException e) {
      throw e;
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("Invalid input: " + e.getMessage(), e);
    } catch (Exception e) {
      throw new RuntimeException("Error fetching task info: " + e.getMessage(), e);
    }
  }

  @Override
  public Task createTask(String instanceId, String condition, Long createUser, Long processUser) {
    try {
      Task task = getTask(instanceId);
      if (task == null) {
        throw new ResourceNotFoundException(
            "Task not found for process instance id: " + instanceId);
      }
      assignTask(task, createUser);
      completeTask(task, condition);

      List<Task> tasks = getTasks(instanceId);

      Task nextTask = getTask(instanceId);
      if (nextTask == null) {
        throw new ResourceNotFoundException(
            "Next task not found for process instance id: " + instanceId);
      }
      assignTask(nextTask, processUser);
      return getTask(instanceId);
    } catch (ResourceNotFoundException e) {
      throw e;
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("Invalid input: " + e.getMessage(), e);
    } catch (Exception e) {
      throw new RuntimeException("Error creating task: " + e.getMessage(), e);
    }
  }

  @Override
  public ProcessTResponse processTask(Long documentId, ProcessTRequest request) {
    try {
      Long processUser;
      ProcessInstance processInstance = getProcess(documentId);
      if (processInstance == null) {
        throw new ResourceNotFoundException(
            "Process instance not found for document id: " + documentId);
      }
      Task task = getTask(processInstance.getId());
      if (task == null) {
        throw new ResourceNotFoundException(
            "Task not found for process instance id: " + processInstance.getId());
      }

      @SuppressWarnings("unchecked")
      Map<String, String> assigneeMap = (Map<String, String>) runtimeService.getVariable(processInstance.getId(),
          "assigneeMap");

      String taskKey = task.getTaskDefinitionKey();
      WorkflowSteps workflowSteps = workflowStepsRepository.findByTaskKeyAndCondition(taskKey, request.getCondition());
      String nextKey = Optional.ofNullable(workflowSteps.getNextKey()).orElse(workflowSteps.getRejectKey());

      if (request.getCondition().contains("REJECT") && !assigneeMap.containsKey(nextKey)) {
        return new ProcessTResponse(processInstance.getProcessDefinitionId(), nextKey, null);
      }

      completeTask(task, request.getCondition());

      Task newTask = getTask(processInstance.getId());
      if (newTask == null) {
        return new ProcessTResponse(processInstance.getProcessDefinitionId(), null, null);
      }
      if (request.getCondition().contains("APPROVE") || request.getCondition().equals("DEFAULT")) {
        processUser = request.getProcessUser();
      } else {
        processUser = Long.valueOf(assigneeMap.get(newTask.getTaskDefinitionKey()));
      }
      assignTask(newTask, processUser);
      return new ProcessTResponse(
          processInstance.getProcessDefinitionId(), newTask.getTaskDefinitionKey(), processUser);
    } catch (ResourceNotFoundException e) {
      throw e;
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("Invalid input: " + e.getMessage(), e);
    } catch (Exception e) {
      throw new RuntimeException("Error processing task: " + e.getMessage(), e);
    }
  }

  public ProcessInstance getProcess(Long documentId) {
    return runtimeService
        .createProcessInstanceQuery()
        .processInstanceBusinessKey(String.valueOf(documentId))
        .singleResult();
  }

  public List<Task> getTasks(String instanceId) {
    return taskService.createTaskQuery().processInstanceId(instanceId).active().list();
  }

  public Task getTask(String instanceId) {
    return taskService.createTaskQuery().processInstanceId(instanceId).active().singleResult();
  }

  public void assignTask(Task task, Long assignedUser) {
    taskService.claim(task.getId(), String.valueOf(assignedUser));

    @SuppressWarnings("unchecked")
    Map<String, String> map = (Map<String, String>) runtimeService.getVariable(task.getProcessInstanceId(),
        "assigneeMap");
    if (map == null)
      map = new HashMap<>();
    if (!map.containsKey(task.getTaskDefinitionKey())) {
      map.put(task.getTaskDefinitionKey(), String.valueOf(assignedUser));
    }
    runtimeService.setVariable(task.getProcessInstanceId(), "assigneeMap", map);
  }

  public void completeTask(Task task, String condition) {
    Map<String, Object> variables = new HashMap<>();
    variables.put("process", condition);
    taskService.complete(task.getId(), variables);
  }
}
