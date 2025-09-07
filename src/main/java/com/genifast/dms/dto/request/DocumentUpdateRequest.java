package com.genifast.dms.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DocumentUpdateRequest {
    @NotBlank
    private String title;
    private String content;
    private String description;
    private String changeDescription;

    @JsonProperty("category_id")
    private Long categoryId;
    // ... các trường metadata khác có thể cập nhật
}