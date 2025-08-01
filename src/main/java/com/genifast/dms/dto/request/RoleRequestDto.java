package com.genifast.dms.dto.request;

import java.util.Set;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RoleRequestDto {
    @NotBlank(message = "Tên vai trò không được để trống")
    private String name;
    private String description;
    @NotNull(message = "ID của tổ chức không được để trống")
    private Long organizationId;
    private Set<Long> permissionIds; // Danh sách ID của các quyền
}