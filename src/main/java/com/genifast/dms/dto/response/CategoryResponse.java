package com.genifast.dms.dto.response;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CategoryResponse {
    private Long id;
    private String name;
    private String description;

    @JsonProperty("parent_category_id")
    private Long parentCategoryId;

    @JsonProperty("department_id")
    private Long departmentId;

    @JsonProperty("organization_id")
    private Long organizationId;
    private Integer status;

    @JsonProperty("created_by")
    private String createdBy;

    @JsonProperty("created_at")
    private Instant createdAt;

    @JsonProperty("updated_at")
    private Instant updatedAt;
}
