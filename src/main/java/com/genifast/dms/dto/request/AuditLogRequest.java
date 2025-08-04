package com.genifast.dms.dto.request;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuditLogRequest {
    private String action;
    private String details;
    private Long documentId;
    private Long userId; // Chỉ lưu ID của user
    private Long delegatedByUserId; // Chỉ lưu ID của người ủy quyền
    private String ipAddress;
    private String sessionId;
}