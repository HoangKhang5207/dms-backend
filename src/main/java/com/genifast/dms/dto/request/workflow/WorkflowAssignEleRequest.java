package com.genifast.dms.dto.request.workflow;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class WorkflowAssignEleRequest {
    @NotNull
    private Long id; // workflowId
    @NotNull
    private WorkflowEleDto workflowEleDto;
}
