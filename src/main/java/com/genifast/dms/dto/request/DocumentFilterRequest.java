package com.genifast.dms.dto.request;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class DocumentFilterRequest {
    private String title;
    private String type;

    @JsonProperty("created_by")
    private String createdBy;

    @JsonProperty("created_from_date")
    private Instant createdFromDate;

    @JsonProperty("created_to_date")
    private Instant createdToDate;
}
