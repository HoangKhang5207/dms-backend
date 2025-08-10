package com.genifast.dms.service.abac.model;

import com.genifast.dms.entity.enums.DocumentConfidentiality;
import lombok.Builder;
import lombok.Data;
import java.time.Instant;
import java.util.List;

/**
 * Thuộc tính của Resource (tài liệu) trong ABAC
 */
@Data
@Builder
public class ResourceAttributes {
    private Long documentId;
    private Long organizationId;
    private Long departmentId;
    private Long projectId;
    private String documentType;
    private DocumentConfidentiality confidentiality;
    private String createdBy;
    private Instant createdDate;
    private List<Long> recipients;
    private Integer status;
    private Integer accessType;
}
