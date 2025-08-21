package com.genifast.dms.dto.bpmn;

import java.time.Instant;
import java.util.List;

import com.genifast.dms.dto.workflow.WorkflowDTO;
import com.genifast.dms.dto.workflow.WorkflowStepsDTO;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BpmnUploadDTO {
  private Long id;

  private int version;

  private String processKey;

  private String name;

  private String path;

  private String pathSvg;

  private Boolean isPublished;

  private Boolean isDeployed;

  private Long organizationId;

  private String createdBy;

  private Instant createdAt;

  private String updatedBy;

  private Instant updatedAt;

  private List<WorkflowDTO> workflowDTOs;

  private List<WorkflowStepsDTO> workflowStepsDTOs;
}
