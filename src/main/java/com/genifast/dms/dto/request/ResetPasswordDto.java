package com.genifast.dms.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResetPasswordDto {
    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 8, max = 20, message = "Mật khẩu phải có độ dài từ 8 đến 20 kí tự")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+}{\":;'?/.,<>]).{8,20}$", message = "Mật khẩu phải chứa ít nhất một chữ hoa, một chữ thường, một số và một kí tự đặc biệt")
    @JsonProperty("new_password")
    private String newPassword;
}
