package com.genifast.dms.dto.response.workflow;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeployWorkflowResponse {
    private String deploymentId;
    private Long workflowId;
}
