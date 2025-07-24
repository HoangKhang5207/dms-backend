package com.genifast.dms.dto.response;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class UserResponse {
    private Long id;
    private String email;

    @JsonProperty("last_name")
    private String lastName;

    @JsonProperty("first_name")
    private String firstName;
    private Integer status;
    private Boolean gender;

    @JsonProperty("is_organization_manager")
    private Boolean isOrganizationManager;

    @JsonProperty("organization_id")
    private Long organizationId;

    @JsonProperty("is_dept_manager")
    private Boolean isDeptManager;

    @JsonProperty("department_id")
    private Long departmentId;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;
}
