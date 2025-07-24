package com.genifast.dms.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CategoryUpdateRequest {

    @NotBlank(message = "Tên thư mục không được bỏ trống")
    private String name;
    private String description;

    @JsonProperty("parent_category_id")
    private Long parentCategoryId;
}
