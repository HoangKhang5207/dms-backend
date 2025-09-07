package com.genifast.dms.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import java.time.Instant;

@Data
@Builder
public class DocumentVersionResponse {
    private Long id;

    @JsonProperty("document_id")
    private Long documentId;

    @JsonProperty("version_number")
    private Integer versionNumber;

    private String title;
    private Integer status;

    @JsonProperty("change_description")
    private String changeDescription;

    @JsonProperty("file_path")
    private String filePath;

    @JsonProperty("created_by")
    private String createdBy;

    @JsonProperty("created_at")
    private Instant createdAt;
}