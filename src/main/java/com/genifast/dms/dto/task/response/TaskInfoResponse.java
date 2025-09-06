package com.genifast.dms.dto.task.response;

import java.util.List;

import com.genifast.dms.dto.bpmn.BpmnUploadDTO;
import com.genifast.dms.dto.workflow.WorkflowEleDTO;
import com.genifast.dms.dto.workflow.WorkflowStepsDTO;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TaskInfoResponse {
    private Long bpmnUpload_id;

    private BpmnUploadDTO bpmnUploadDto;

    private WorkflowEleDTO workflowEleDto;

    private List<WorkflowStepsDTO> workflowStepDtos;
}
