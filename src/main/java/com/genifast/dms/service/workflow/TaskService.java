package com.genifast.dms.service.workflow;

import com.genifast.dms.dto.request.workflow.TaskProcessRequest;
import com.genifast.dms.dto.response.workflow.ProcessTResponse;

public interface TaskService {
    ProcessTResponse process(Long documentId, TaskProcessRequest req);
}
