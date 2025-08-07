package com.genifast.dms.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import java.util.Set;

@Data
public class ProjectRoleRequest {
    @NotBlank(message = "Tên vai trò không được để trống")
    private String name;

    private String description;

    @NotEmpty(message = "Vai trò phải có ít nhất một quyền")
    private Set<Long> permissionIds;
}