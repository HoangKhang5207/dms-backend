package com.genifast.dms.dto.workflow;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;

import com.genifast.dms.dto.bpmn.BpmnUploadDTO;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowDTO {
    private Long id;

    private int version;

    @NotNull
    private String documentType;

    @NotNull
    private String name;

    @NotNull
    private String description;

    private String startedRole;

    private List<Long> departmentIds;

    private String createdBy;

    private Instant createdAt;

    private String updatedBy;

    private Instant updatedAt;

    private Long bpmnUploadId;

    private BpmnUploadDTO bpmnUploadDto;

    private WorkflowEleDTO workflowEleDto;
}
