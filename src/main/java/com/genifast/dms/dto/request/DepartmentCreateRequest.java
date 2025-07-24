package com.genifast.dms.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DepartmentCreateRequest {
    @NotBlank(message = "Tên phòng ban không được để trống")
    private String name;

    @NotBlank(message = "Mô tả không được để trống")
    private String description;

    @JsonProperty("organization_id")
    @NotNull(message = "ID của tổ chức là bắt buộc")
    private Long organizationId;
}