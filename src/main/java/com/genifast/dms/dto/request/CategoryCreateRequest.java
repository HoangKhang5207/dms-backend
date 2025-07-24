package com.genifast.dms.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CategoryCreateRequest {
    @NotBlank(message = "Tên thư mục không được để trống")
    private String name;

    @NotBlank(message = "Mô tả không được để trống")
    private String description;

    @JsonProperty("parent_category_id")
    private Long parentCategoryId;

    @JsonProperty("department_id")
    @NotNull(message = "Id phòng ban không được để trống")
    private Long departmentId;
}
