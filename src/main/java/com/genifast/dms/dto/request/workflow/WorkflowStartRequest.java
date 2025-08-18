package com.genifast.dms.dto.request.workflow;

import lombok.Data;

@Data
public class WorkflowStartRequest {
    private Long workflowId;
    private Long documentId;
    private String condition; // DEFAULT
    private Long startUser;
    private Long processUser;
}
