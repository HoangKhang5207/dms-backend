package com.genifast.dms.controller;

import com.genifast.dms.dto.response.AuditLogResponse;
import com.genifast.dms.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping
    @PreAuthorize("hasAuthority('audit:log')")
    public ResponseEntity<Page<AuditLogResponse>> getAllLogs(@PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(auditLogService.getLogs(pageable));
    }

    @GetMapping("/document/{docId}")
    @PreAuthorize("hasAuthority('audit:log')")
    public ResponseEntity<Page<AuditLogResponse>> getLogsByDocument(
            @PathVariable Long docId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(auditLogService.getLogsByDocument(docId, pageable));
    }
}