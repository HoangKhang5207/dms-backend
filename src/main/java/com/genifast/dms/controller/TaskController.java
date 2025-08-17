package com.genifast.dms.controller;

import static com.genifast.dms.common.constant.MessageCode.CommonMessage.SUCCESS_CREATE_DATA;
import static com.genifast.dms.common.constant.MessageCode.CommonMessage.SUCCESS_GET_DATA;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.genifast.dms.common.exception.ResourceNotFoundException;
import com.genifast.dms.dto.BaseResponseDto;
import com.genifast.dms.dto.task.request.ProcessTRequest;
import com.genifast.dms.dto.task.response.ProcessTResponse;
import com.genifast.dms.dto.task.response.TaskInfoResponse;
import com.genifast.dms.service.task.WTaskService;

@RestController
@RequestMapping("api/v1/tasks")
@RequiredArgsConstructor
public class TaskController extends BaseController {
    private final WTaskService taskService;

    @GetMapping("/document/{document_id}")
    public ResponseEntity<BaseResponseDto> getTaskInfo(@PathVariable("document_id") Long documentId) {
        try {
            TaskInfoResponse response = taskService.getTaskInfo(documentId);
            return success(response, SUCCESS_GET_DATA);
        } catch (ResourceNotFoundException e) {
            return notFound(e.getMessage());
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        } catch (Exception e) {
            return internalServerError("Error fetching task info: " + e.getMessage());
        }
    }

    @PostMapping("/document/{document_id}/process")
    @Transactional
    public ResponseEntity<BaseResponseDto> processTask(
            @PathVariable("document_id") Long documentId, @RequestBody ProcessTRequest request) {
        try {
            ProcessTResponse response = taskService.processTask(documentId, request);
            return success(response, SUCCESS_CREATE_DATA);
        } catch (ResourceNotFoundException e) {
            return notFound(e.getMessage());
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        } catch (Exception e) {
            return internalServerError("Error processing task: " + e.getMessage());
        }
    }
}
