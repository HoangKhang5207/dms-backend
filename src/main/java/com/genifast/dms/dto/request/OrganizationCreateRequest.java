package com.genifast.dms.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class OrganizationCreateRequest {
    @NotBlank(message = "Tên của tổ chức là bắt buộc")
    @Size(max = 255, message = "Độ dài quá lớn, vui lòng nhập tên nhỏ hơn hoặc bằng 255 kí tự")
    private String name;

    @Size(max = 255, message = "Độ dài quá lớn, vui lòng nhập mô tả nhỏ hơn hoặc bằng 255 kí t")
    private String description;

    @JsonProperty("is_openai")
    private Boolean isOpenai;
}
