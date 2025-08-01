package com.genifast.dms.dto.response;

import java.util.Set;
import lombok.Data;

@Data
public class RoleResponseDto {
    private Long id;
    private String name;
    private String description;
    private Long organizationId;
    private Set<PermissionResponseDto> permissions; // Trả về thông tin chi tiết của quyền
}