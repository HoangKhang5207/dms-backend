package com.genifast.dms.dto.response;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class DepartmentResponse {
    private Long id;
    private String name;
    private String description;
    private Integer status;

    @JsonProperty("organization_id")
    private Long organizationId;

    @JsonProperty("created_by")
    private String createdBy;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;
}