package com.genifast.dms.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import java.time.Instant;

@Data
@Builder
public class AuditLogResponse {
    private Long id;

    @JsonProperty("user_email")
    private String userEmail;

    @JsonProperty("document_id")
    private Long documentId;

    private String action;
    private Instant timestamp;
    private String details;

    @JsonProperty("ip_address")
    private String ipAddress;

    @JsonProperty("session_id")
    private String sessionId;
}