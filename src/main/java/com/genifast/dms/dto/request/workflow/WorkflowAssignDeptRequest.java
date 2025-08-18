package com.genifast.dms.dto.request.workflow;

import java.util.List;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class WorkflowAssignDeptRequest {
    @NotNull
    private Long id; // workflowId
    @NotNull
    private List<Long> departmentIds;
}
