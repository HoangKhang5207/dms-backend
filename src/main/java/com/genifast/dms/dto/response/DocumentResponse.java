package com.genifast.dms.dto.response;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class DocumentResponse {
    private Long id;
    private String title;
    private String description;
    private Integer status;
    private String type;

    @JsonProperty("access_type")
    private Integer accessType;

    @JsonProperty("category_id")
    private Long categoryId;

    @JsonProperty("department_id")
    private Long departmentId;

    @JsonProperty("organization_id")
    private Long organizationId;

    @JsonProperty("original_filename")
    private String originalFilename;

    @JsonProperty("storage_unit")
    private String storageUnit;

    @JsonProperty("file_path")
    private String filePath;

    @JsonProperty("created_by")
    private String createdBy;

    @JsonProperty("created_at")
    private Instant createdAt;

    @JsonProperty("updated_at")
    private Instant updatedAt;
}
