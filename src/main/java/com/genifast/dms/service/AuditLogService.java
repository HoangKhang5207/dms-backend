package com.genifast.dms.service;

import com.genifast.dms.dto.request.AuditLogRequest;
import com.genifast.dms.dto.response.AuditLogResponse;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AuditLogService {
    void logAction(AuditLogRequest logRequest);

    Page<AuditLogResponse> getLogs(Pageable pageable);

    Page<AuditLogResponse> getLogsByDocument(Long documentId, Pageable pageable);
}