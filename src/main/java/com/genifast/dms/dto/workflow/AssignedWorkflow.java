package com.genifast.dms.dto.workflow;

import com.genifast.dms.entity.bpmn.BpmnUpload;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AssignedWorkflow {
  private Long id;

  private String name;

  private String categoryIds;

  private String urgency;

  private String security;

  private BpmnUpload bpmnUpload;

  private WorkflowStepsDTO workflowStepsDto;
}
