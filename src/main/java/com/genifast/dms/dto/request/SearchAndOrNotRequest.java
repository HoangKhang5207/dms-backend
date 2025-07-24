package com.genifast.dms.dto.request;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class SearchAndOrNotRequest {
    @JsonProperty("and_keywords")
    private List<String> andKeywords;

    @JsonProperty("or_keywords")
    private List<String> orKeywords;

    @JsonProperty("not_keywords")
    private List<String> notKeywords;
}
