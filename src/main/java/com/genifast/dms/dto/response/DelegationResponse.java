package com.genifast.dms.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import java.time.Instant;

@Data
@Builder
public class DelegationResponse {
    private Long id;

    @JsonProperty("delegator_id")
    private Long delegatorId;

    @JsonProperty("delegatee_id")
    private Long delegateeId;

    @JsonProperty("document_id")
    private Long documentId;

    private String permission;

    @JsonProperty("expiry_at")
    private Instant expiryAt;

    @JsonProperty("created_at")
    private Instant createdAt;
}