package com.genifast.dms.dto.response;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class OrganizationResponse {
    private Long id;
    private String name;
    private String description;
    private Integer status;

    @JsonProperty("is_openai")
    private Boolean isOpenai;

    @JsonProperty("limit_data")
    private Long limitData;

    @JsonProperty("data_used")
    private Long dataUsed;

    @JsonProperty("limit_token")
    private Long limitToken;

    @JsonProperty("token_used")
    private Long tokenUsed;

    @JsonProperty("percent_data_used")
    private Long percentDataUsed;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @JsonProperty("created_by")
    private String createdBy;

    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;

    @JsonProperty("updated_by")
    private String updatedBy;
}
