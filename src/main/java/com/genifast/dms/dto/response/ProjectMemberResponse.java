package com.genifast.dms.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ProjectMemberResponse {
    @JsonProperty("membership_id") // ID của bản ghi project_members
    private Long id;

    @JsonProperty("user_id")
    private Long userId;

    @JsonProperty("user_email")
    private String userEmail;

    @JsonProperty("user_full_name")
    private String userFullName; // Thêm tên đầy đủ để hiển thị

    @JsonProperty("project_role_id")
    private Long projectRoleId;

    @JsonProperty("project_role_name")
    private String projectRoleName;
}