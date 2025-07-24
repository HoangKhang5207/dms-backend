package com.genifast.dms.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DepartmentUpdateRequest {
    @NotBlank(message = "Tên phòng ban không được để trống")
    private String name;

    @NotBlank(message = "Mô tả không được để trống")
    private String description;
}