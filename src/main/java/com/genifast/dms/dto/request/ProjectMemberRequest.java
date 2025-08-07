package com.genifast.dms.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ProjectMemberRequest {
    @NotNull(message = "User ID không được để trống")
    @JsonProperty("user_id")
    private Long userId;

    @NotNull(message = "Vai trò trong dự án không được để trống")
    @JsonProperty("project_role_id")
    private Long projectRoleId;
}