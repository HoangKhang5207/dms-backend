package com.genifast.dms.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.genifast.dms.dto.request.workflow.TaskProcessRequest;
import com.genifast.dms.dto.response.workflow.ProcessTResponse;
import com.genifast.dms.service.workflow.TaskService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
@Validated
public class TaskController {

    private final TaskService taskService;

    // POST /api/v1/tasks/document/{documentId}/process
    @PostMapping("/document/{documentId}/process")
    public ResponseEntity<ProcessTResponse> process(
            @PathVariable("documentId") Long documentId,
            @Valid @RequestBody TaskProcessRequest req) {
        ProcessTResponse resp = taskService.process(documentId, req);
        return ResponseEntity.ok(resp);
    }
}
