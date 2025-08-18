package com.genifast.dms.dto.request.workflow;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class WorkflowDeployRequest {
    @NotNull
    private Long bpmnUploadId;
    @NotNull
    private String documentType;
    @NotNull
    private String name;
    private String description;
}
