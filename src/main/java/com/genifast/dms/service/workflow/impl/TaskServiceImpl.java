package com.genifast.dms.service.workflow.impl;

import java.util.HashMap;
import java.util.Map;

import org.flowable.engine.RuntimeService;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Service;

import com.genifast.dms.dto.request.workflow.TaskProcessRequest;
import com.genifast.dms.dto.response.workflow.ProcessTResponse;
import com.genifast.dms.service.workflow.TaskService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {

    private final RuntimeService runtimeService;
    private final org.flowable.engine.TaskService flowableTaskService;

    @Override
    public ProcessTResponse process(Long documentId, TaskProcessRequest req) {
        log.info("[TASK] process documentId={}, condition={}, processUserNext={}", documentId, req.getCondition(), req.getProcessUser());

        String businessKey = String.valueOf(documentId);
        ProcessInstance pi = runtimeService.createProcessInstanceQuery()
                .processInstanceBusinessKey(businessKey)
                .singleResult();
        if (pi == null) {
            throw new IllegalStateException("No active process instance for documentId=" + documentId);
        }
        log.info("[TASK][FOUND_PI] businessKey={}, procInstId={}, processKey={}", businessKey, pi.getId(), pi.getProcessDefinitionKey());

        Task current = flowableTaskService.createTaskQuery()
                .processInstanceId(pi.getId())
                .active()
                .orderByTaskCreateTime()
                .asc()
                .singleResult();

        if (current == null) {
            // already completed
            return ProcessTResponse.builder()
                    .processKey(pi.getProcessDefinitionKey())
                    .taskKeyNext(null)
                    .processUserNext(null)
                    .build();
        }

        Map<String, Object> completeVars = new HashMap<>();
        if (req.getCondition() != null) completeVars.put("condition", req.getCondition());
        log.info("[TASK][CURRENT] taskId={}, taskKey={}, currentAssignee={}, completeVars={}", current.getId(), current.getTaskDefinitionKey(), current.getAssignee(), completeVars);
        try {
            Map<String, Object> varsBefore = runtimeService.getVariables(pi.getId());
            log.info("[TASK][VARS_BEFORE] {}", varsBefore);
        } catch (Exception e) {
            log.warn("[TASK] cannot read variables before complete: {}", e.getMessage());
        }
        flowableTaskService.complete(current.getId(), completeVars);
        log.info("[TASK][COMPLETE] completed taskId={} with condition={}", current.getId(), req.getCondition());

        Task next = flowableTaskService.createTaskQuery()
                .processInstanceId(pi.getId())
                .active()
                .orderByTaskCreateTime()
                .asc()
                .singleResult();

        String nextKey = next != null ? next.getTaskDefinitionKey() : null;
        Long nextUser = req.getProcessUser();
        log.info("[TASK][NEXT] nextTaskId={}, nextTaskKey={}", next != null ? next.getId() : null, nextKey);
        if (next != null && nextUser != null) {
            try {
                flowableTaskService.setAssignee(next.getId(), String.valueOf(nextUser));
                log.info("[TASK][ASSIGN_NEXT] taskId={}, assignee={}", next.getId(), nextUser);
            } catch (Exception e) {
                log.warn("[TASK] setAssignee failed for task {}: {}", next.getId(), e.getMessage());
            }
        }

        // update assigneeMap variable
        if (next != null) {
            try {
                Object mapObj = runtimeService.getVariable(pi.getId(), "assigneeMap");
                if (mapObj instanceof Map<?, ?> m) {
                    @SuppressWarnings("unchecked") Map<String, Object> cast = (Map<String, Object>) m;
                    cast.put(nextKey, nextUser);
                    runtimeService.setVariable(pi.getId(), "assigneeMap", cast);
                    log.info("[TASK][VARS] assigneeMap[{}] updated to {}", nextKey, nextUser);
                }
                Map<String, Object> varsAfter = runtimeService.getVariables(pi.getId());
                log.info("[TASK][VARS_AFTER] {}", varsAfter);
            } catch (Exception e) {
                log.warn("[TASK] update assigneeMap failed: {}", e.getMessage());
            }
        }

        return ProcessTResponse.builder()
                .processKey(pi.getProcessDefinitionKey())
                .taskKeyNext(nextKey)
                .processUserNext(nextUser)
                .build();
    }
}
