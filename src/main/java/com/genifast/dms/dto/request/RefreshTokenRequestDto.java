package com.genifast.dms.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RefreshTokenRequestDto {
    @JsonProperty("refresh_token")
    @NotBlank(message = "Refresh token không được để trống")
    private String refreshToken;
}
