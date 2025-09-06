package com.genifast.dms.service.abac.model;

import lombok.Builder;
import lombok.Data;
import java.util.Set;

/**
 * Thuộc tính của Subject (người dùng) trong ABAC
 */
@Data
@Builder
public class SubjectAttributes {
    private Long userId;
    private Long organizationId;
    private Long departmentId;
    private String departmentCode;
    private Set<String> roles;
    private Integer level;
    private boolean isAdmin;
    private boolean isOrganizationManager;
    private boolean isDepartmentManager;
    private String currentDeviceId;
}
