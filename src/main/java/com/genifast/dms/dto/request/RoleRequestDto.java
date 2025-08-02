package com.genifast.dms.dto.request;

import java.util.Set;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RoleRequestDto {
    @NotBlank(message = "Tên vai trò không được để trống")
    private String name;
    private String description;

    private Long organizationId;
    private Set<Long> permissionIds; // Danh sách ID của các quyền
}