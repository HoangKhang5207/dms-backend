package com.genifast.dms.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.Set;

@Data
public class ProjectRoleResponse {
    private Long id;
    private String name;
    private String description;

    @JsonProperty("project_id")
    private Long projectId; // Thêm projectId để biết vai trò này thuộc dự án nào

    private Set<PermissionResponseDto> permissions;
}