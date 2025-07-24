package com.genifast.dms.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CheckOrgResponse {

    @JsonProperty("has_pending_request")
    private boolean hasPendingRequest;
}
