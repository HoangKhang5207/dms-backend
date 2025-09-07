package com.genifast.dms.dto.response;

import java.time.Instant;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class DocumentResponse {
    private Long id;
    private String title;
    private String description;
    private Integer status;
    private String type;

    // Bổ sung: Thêm trường này để frontend biết rõ loại tài liệu nghiệp vụ
    @JsonProperty("document_type")
    private String documentType;

    // Bổ sung: Phiên bản mới nhất của tài liệu
    @JsonProperty("latest_version")
    private Integer latestVersion;

    @JsonProperty("access_type")
    private Integer accessType;

    @JsonProperty("category_id")
    private Long categoryId;

    // Bổ sung: Thêm tên category để tiện hiển thị
    @JsonProperty("category_name")
    private String categoryName;

    @JsonProperty("department_id")
    private Long departmentId;

    @JsonProperty("organization_id")
    private Long organizationId;

    // Bổ sung: ID của dự án nếu tài liệu thuộc về một dự án
    @JsonProperty("project_id")
    private Long projectId;

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

    // Bổ sung: Danh sách người nhận (chỉ ID hoặc email) để frontend biết ai có
    // quyền truy cập
    private Set<String> recipients;
}
