package com.genifast.dms.dto.response.workflow;

import lombok.Data;

@Data
public class BpmnUploadResponse {
    private Long id;
    private String name;
    private Integer version;
    private String processKey;
    private String path;
    private String pathSvg;
    private Boolean isPublished;
    private Boolean isDeployed;
    private Long organizationId;
}
