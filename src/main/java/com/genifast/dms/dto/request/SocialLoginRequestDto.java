package com.genifast.dms.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class SocialLoginRequestDto {
    @NotBlank(message = "Email không được để trống")
    @Pattern(regexp = "^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$", message = "Định dạng email không hợp lệ")
    private String email;

    @JsonProperty("first_name")
    @NotBlank(message = "Tên không được để trống")
    private String firstName;

    @JsonProperty("last_name")
    @NotBlank(message = "Họ không được để trống")
    private String lastName;
}
