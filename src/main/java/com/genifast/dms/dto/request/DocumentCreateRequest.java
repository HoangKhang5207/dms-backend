package com.genifast.dms.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DocumentCreateRequest {
    private String title;
    private String content;
    private String description;

    @JsonProperty("category_id")
    @NotNull(message = "Id của thư mục không được để trống")
    private Long categoryId;

    @JsonProperty("access_type")
    @NotNull(message = "Phạm vi truy cập không được để trống")
    private Integer accessType;

    @JsonProperty("total_page")
    private Integer totalPage;
}
