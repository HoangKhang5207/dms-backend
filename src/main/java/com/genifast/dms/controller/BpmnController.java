package com.genifast.dms.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.genifast.dms.dto.response.workflow.BpmnUploadResponse;
import com.genifast.dms.service.workflow.BpmnService;

import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/bpmn")
@RequiredArgsConstructor
@Validated
public class BpmnController {

    private final BpmnService bpmnService;

    // POST /api/v1/bpmn/organization/{organizationId}/save
    @PostMapping(value = "/organization/{organizationId}/save", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BpmnUploadResponse> saveBpmn(
            @PathVariable("organizationId") Long organizationId,
            @RequestPart("name") String name,
            @RequestPart("file") MultipartFile bpmnFile,
            @RequestPart(value = "svgFile", required = false) MultipartFile svgFile,
            @RequestParam(value = "bpmnUploadId", required = false) Long bpmnUploadId,
            @RequestParam(value = "isPublished", required = false, defaultValue = "false") Boolean isPublished) {
        BpmnUploadResponse resp = bpmnService.saveBpmn(organizationId, name, bpmnFile, svgFile, bpmnUploadId, isPublished);
        return new ResponseEntity<>(resp, HttpStatus.CREATED);
    }

    // GET /api/v1/bpmn/organization/{organizationId}
    @GetMapping("/organization/{organizationId}")
    public ResponseEntity<List<BpmnUploadResponse>> listBpmnByOrg(@PathVariable("organizationId") Long organizationId) {
        return ResponseEntity.ok(bpmnService.listByOrganization(organizationId));
    }

    // DELETE /api/v1/bpmn/organization/{organizationId}/bpmn_upload/{id}
    @DeleteMapping("/organization/{organizationId}/bpmn_upload/{id}")
    public ResponseEntity<Void> softDelete(
            @PathVariable("organizationId") Long organizationId,
            @PathVariable("id") @NotNull Long id) {
        bpmnService.softDelete(organizationId, id);
        return ResponseEntity.noContent().build();
    }
}
