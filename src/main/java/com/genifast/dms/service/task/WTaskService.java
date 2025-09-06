package com.genifast.dms.service.task;

import org.camunda.bpm.engine.task.Task;

import com.genifast.dms.dto.task.request.ProcessTRequest;
import com.genifast.dms.dto.task.response.ProcessTResponse;
import com.genifast.dms.dto.task.response.TaskInfoResponse;

public interface WTaskService {
  TaskInfoResponse getTaskInfo(Long documentId);

  Task createTask(String instanceId, String condition, Long createUser, Long processUser);

  ProcessTResponse processTask(Long documentId, ProcessTRequest request);
}
