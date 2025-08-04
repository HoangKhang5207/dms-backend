package com.genifast.dms.service;

import com.genifast.dms.dto.response.AuditLogResponse;
import com.genifast.dms.entity.User;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AuditLogService {
    void logAction(String action, String details, Long documentId, User user);

    Page<AuditLogResponse> getLogs(Pageable pageable);

    Page<AuditLogResponse> getLogsByDocument(Long documentId, Pageable pageable);
}