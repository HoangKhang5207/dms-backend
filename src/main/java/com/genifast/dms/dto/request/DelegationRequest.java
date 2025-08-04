package com.genifast.dms.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.Instant;

@Data
public class DelegationRequest {
    @NotNull(message = "ID người được ủy quyền là bắt buộc")
    @JsonProperty("delegatee_id")
    private Long delegateeId;

    @NotNull(message = "ID tài liệu là bắt buộc")
    @JsonProperty("document_id")
    private Long documentId;

    @NotNull(message = "Quyền ủy quyền là bắt buộc")
    @NotBlank(message = "Quyền ủy quyền không được để trống")
    private String permission;

    @JsonProperty("expiry_at")
    private Instant expiryAt; // Thời gian hết hạn (có thể null)
}