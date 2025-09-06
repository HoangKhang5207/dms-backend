package com.genifast.dms.service.workflow;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.genifast.dms.dto.workflow.WorkflowStepsDTO;
import com.genifast.dms.entity.workflow.WorkflowSteps;

@Service
@AllArgsConstructor
@Slf4j
public class WorkflowStepsServiceImpl implements WorkflowStepsService {

  @Override
  public List<WorkflowStepsDTO> getWorkflowStepsDtos(
      List<WorkflowStepsDTO> currentWStepDtos, List<WorkflowSteps> nextWSteps) {

    List<WorkflowSteps> uniqueTaskKeySteps = new ArrayList<>(
        nextWSteps.stream()
            .collect(
                Collectors.toMap(
                    WorkflowSteps::getTaskKey, // key: taskKey
                    step -> step, // value: bản ghi
                    (existing, replacement) -> existing // nếu trùng key, giữ bản ghi đầu tiên
                ))
            .values());

    List<WorkflowStepsDTO> workflowStepsDtos = new ArrayList<>();
    for (WorkflowStepsDTO currentWStepDto : currentWStepDtos) {
      for (WorkflowSteps workflowStep : uniqueTaskKeySteps) {
        if (Objects.equals(workflowStep.getTaskKey(), currentWStepDto.getNextKey())
            || Objects.equals(workflowStep.getTaskKey(), currentWStepDto.getRejectKey())) {
          currentWStepDto.setProcessUser(workflowStep.getCandidateGroup());
          workflowStepsDtos.add(currentWStepDto);
        }
      }
    }
    return workflowStepsDtos;
  }
}
