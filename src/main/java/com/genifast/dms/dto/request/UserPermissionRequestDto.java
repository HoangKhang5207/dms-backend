package com.genifast.dms.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UserPermissionRequestDto {
    @NotNull(message = "Quyền không được để trống.")
    private Long permissionId;
}