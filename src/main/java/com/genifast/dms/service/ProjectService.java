package com.genifast.dms.service;

import com.genifast.dms.dto.request.ProjectCreateRequest;
import com.genifast.dms.dto.request.ProjectMemberRequest;
import com.genifast.dms.dto.request.ProjectRoleRequest;
import com.genifast.dms.dto.request.ProjectUpdateRequest;
import com.genifast.dms.dto.response.ProjectResponse;
import com.genifast.dms.dto.response.ProjectRoleResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProjectService {

    // === Quản lý Dự án ===
    ProjectResponse createProject(ProjectCreateRequest createDto);

    ProjectResponse updateProject(Long projectId, ProjectUpdateRequest updateDto);

    ProjectResponse getProjectById(Long projectId);

    Page<ProjectResponse> getProjectsByOrganization(Pageable pageable);

    void deleteProject(Long projectId); // Có thể là xóa mềm

    // === Quản lý Thành viên Dự án ===
    ProjectResponse addMemberToProject(Long projectId, ProjectMemberRequest addDto);

    ProjectResponse removeMemberFromProject(Long projectId, Long userId);

    ProjectResponse changeMemberRole(Long projectId, Long userId, Long newProjectRoleId);

    // === Quản lý Vai trò trong Dự án ===
    ProjectRoleResponse createProjectRole(Long projectId, ProjectRoleRequest roleDto);

    ProjectRoleResponse updateProjectRole(Long projectId, Long projectRoleId, ProjectRoleRequest roleDto);

    void deleteProjectRole(Long projectId, Long projectRoleId);

    // === Tác vụ nền (Scheduled Task) ===
    void updateExpiredProjectsStatus();
}