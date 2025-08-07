package com.genifast.dms.service.impl;

import com.genifast.dms.common.constant.ErrorCode;
import com.genifast.dms.common.constant.ErrorMessage;
import com.genifast.dms.common.exception.ApiException;
import com.genifast.dms.common.utils.JwtUtils;
import com.genifast.dms.dto.request.*;
import com.genifast.dms.dto.response.*;
import com.genifast.dms.entity.*;
import com.genifast.dms.mapper.ProjectMapper;
import com.genifast.dms.mapper.ProjectRoleMapper;
import com.genifast.dms.repository.*;
import com.genifast.dms.service.ProjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final ProjectRoleRepository projectRoleRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final PermissionRepository permissionRepository;
    private final ProjectMapper projectMapper;
    private final ProjectRoleMapper projectRoleMapper;

    // === Quản lý Dự án ===

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('organization:manage')") // Giả sử có quyền này cho Org Manager
    public ProjectResponse createProject(ProjectCreateRequest createDto) {
        User currentUser = getCurrentUser();
        Organization org = currentUser.getOrganization();
        if (org == null) {
            throw new ApiException(ErrorCode.USER_NOT_IN_ORGANIZATION,
                    ErrorMessage.USER_NOT_IN_ORGANIZATION.getMessage());
        }

        if (createDto.getStartDate().isAfter(createDto.getEndDate())) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "Ngày bắt đầu không được sau ngày kết thúc.");
        }

        Project project = Project.builder()
                .name(createDto.getName())
                .description(createDto.getDescription())
                .startDate(createDto.getStartDate())
                .endDate(createDto.getEndDate())
                .status(1) // 1 = ACTIVE
                .organization(org)
                .build();

        Project savedProject = projectRepository.save(project);

        // Tự động tạo vai trò mặc định: Trưởng dự án & Thành viên
        createDefaultRolesForProject(savedProject);

        log.info("User '{}' created project '{}' (ID: {}) in organization ID {}", currentUser.getEmail(),
                savedProject.getName(), savedProject.getId(), org.getId());
        return projectMapper.toProjectResponse(savedProject);
    }

    private void createDefaultRolesForProject(Project project) {
        // Vai trò Trưởng dự án (Full control trong dự án)
        Set<Permission> leadPermissions = new HashSet<>(permissionRepository.findAllByNameIn(List.of(
                "documents:read", "documents:create", "documents:update", "documents:delete",
                "documents:download", "documents:upload", "documents:comment"
        // Thêm các quyền quản lý thành viên dự án sẽ được tạo sau
        )));
        ProjectRole leadRole = ProjectRole.builder()
                .project(project)
                .name("Trưởng dự án")
                .description("Quản lý và điều hành toàn bộ dự án.")
                .permissions(leadPermissions)
                .build();
        projectRoleRepository.save(leadRole);

        // Vai trò Thành viên (Quyền cộng tác cơ bản)
        Set<Permission> memberPermissions = new HashSet<>(permissionRepository.findAllByNameIn(List.of(
                "documents:read", "documents:upload", "documents:comment")));
        ProjectRole memberRole = ProjectRole.builder()
                .project(project)
                .name("Thành viên")
                .description("Thành viên tham gia thực hiện công việc trong dự án.")
                .permissions(memberPermissions)
                .build();
        projectRoleRepository.save(memberRole);
    }

    @Override
    @Transactional
    @PreAuthorize("hasPermission(#projectId, 'project', 'project:manage')")
    public ProjectResponse updateProject(Long projectId, ProjectUpdateRequest updateDto) {
        Project project = findProjectById(projectId);

        if (updateDto.getStartDate().isAfter(updateDto.getEndDate())) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "Ngày bắt đầu không được sau ngày kết thúc.");
        }

        project.setName(updateDto.getName());
        project.setDescription(updateDto.getDescription());
        project.setStartDate(updateDto.getStartDate());
        project.setEndDate(updateDto.getEndDate());
        project.setStatus(updateDto.getStatus());

        Project updatedProject = projectRepository.save(project);
        log.info("Project ID {} updated by user {}", projectId, getCurrentUser().getEmail());
        return projectMapper.toProjectResponse(updatedProject);
    }

    @Override
    @PreAuthorize("hasPermission(#projectId, 'project', 'project:read')")
    public ProjectResponse getProjectById(Long projectId) {
        Project project = findProjectById(projectId);
        return projectMapper.toProjectResponse(project);
    }

    @Override
    public Page<ProjectResponse> getProjectsByOrganization(Pageable pageable) {
        // Logic để lấy danh sách dự án của tổ chức hiện tại
        // Sẽ được cài đặt sau khi có Controller
        return Page.empty();
    }

    @Override
    @Transactional
    @PreAuthorize("hasPermission(#projectId, 'project', 'project:manage')")
    public void deleteProject(Long projectId) {
        Project project = findProjectById(projectId);
        // Thay vì xóa cứng, chúng ta có thể đổi trạng thái sang DELETED
        // Hoặc đơn giản là xóa nó đi
        projectRepository.delete(project);
        log.warn("Project ID {} deleted by user {}", projectId, getCurrentUser().getEmail());
    }

    // === Quản lý Thành viên Dự án ===

    @Override
    @Transactional
    @PreAuthorize("hasPermission(#projectId, 'project', 'project:member:manage')")
    public ProjectResponse addMemberToProject(Long projectId, ProjectMemberRequest addDto) {
        Project project = findProjectById(projectId);
        User currentUser = getCurrentUser();

        User userToAdd = userRepository.findById(addDto.getUserId())
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND, ErrorMessage.INVALID_USER.getMessage()));

        ProjectRole projectRole = projectRoleRepository.findById(addDto.getProjectRoleId())
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_REQUEST, "Vai trò trong dự án không tồn tại."));

        // Validation:
        // 1. User được thêm phải cùng tổ chức với dự án
        if (userToAdd.getOrganization() == null
                || !userToAdd.getOrganization().getId().equals(project.getOrganization().getId())) {
            throw new ApiException(ErrorCode.USER_NOT_IN_ORGANIZATION, "Thành viên phải thuộc cùng tổ chức với dự án.");
        }
        // 2. Vai trò phải thuộc dự án này
        if (!projectRole.getProject().getId().equals(projectId)) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "Vai trò không thuộc về dự án này.");
        }
        // 3. Kiểm tra thành viên đã tồn tại trong dự án chưa
        boolean isAlreadyMember = project.getMembers().stream()
                .anyMatch(member -> member.getUser().getId().equals(addDto.getUserId()));
        if (isAlreadyMember) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "Người dùng đã là thành viên của dự án.");
        }

        ProjectMember newMember = ProjectMember.builder()
                .project(project)
                .user(userToAdd)
                .projectRole(projectRole)
                .build();

        projectMemberRepository.save(newMember);

        // Tải lại project để có danh sách thành viên mới nhất
        Project updatedProject = findProjectById(projectId);

        log.info("User '{}' added user '{}' to project ID {} with role '{}'", currentUser.getEmail(),
                userToAdd.getEmail(), projectId, projectRole.getName());
        return projectMapper.toProjectResponse(updatedProject);
    }

    @Override
    @Transactional
    @PreAuthorize("hasPermission(#projectId, 'project', 'project:member:manage')")
    public ProjectResponse removeMemberFromProject(Long projectId, Long userIdToRemove) {
        Project project = findProjectById(projectId);
        User currentUser = getCurrentUser();

        // Tìm thành viên cần xóa trong danh sách thành viên của dự án
        ProjectMember memberToRemove = project.getMembers().stream()
                .filter(member -> member.getUser().getId().equals(userIdToRemove))
                .findFirst()
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND,
                        "Người dùng không phải là thành viên của dự án."));

        // Validation: Không cho phép tự xóa mình
        if (currentUser.getId().equals(userIdToRemove)) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "Bạn không thể tự xóa chính mình khỏi dự án.");
        }

        projectMemberRepository.delete(memberToRemove);

        Project updatedProject = findProjectById(projectId);
        log.warn("User '{}' removed user ID {} from project ID {}", currentUser.getEmail(), userIdToRemove, projectId);
        return projectMapper.toProjectResponse(updatedProject);
    }

    @Override
    @Transactional
    @PreAuthorize("hasPermission(#projectId, 'project', 'project:member:manage')")
    public ProjectResponse changeMemberRole(Long projectId, Long userId, Long newProjectRoleId) {
        Project project = findProjectById(projectId);
        User currentUser = getCurrentUser();

        ProjectMember memberToUpdate = project.getMembers().stream()
                .filter(member -> member.getUser().getId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND,
                        "Người dùng không phải là thành viên của dự án."));

        ProjectRole newRole = project.getRoles().stream()
                .filter(role -> role.getId().equals(newProjectRoleId))
                .findFirst()
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_REQUEST, "Vai trò mới không thuộc dự án này."));

        String oldRoleName = memberToUpdate.getProjectRole().getName();
        memberToUpdate.setProjectRole(newRole);
        projectMemberRepository.save(memberToUpdate);

        Project updatedProject = findProjectById(projectId);
        log.info("User '{}' changed role of user ID {} in project ID {} from '{}' to '{}'", currentUser.getEmail(),
                userId, projectId, oldRoleName, newRole.getName());
        return projectMapper.toProjectResponse(updatedProject);
    }

    // === Quản lý Vai trò trong Dự án ===

    @Override
    @Transactional
    @PreAuthorize("hasPermission(#projectId, 'project', 'project:role:manage')")
    public ProjectRoleResponse createProjectRole(Long projectId, ProjectRoleRequest roleDto) {
        Project project = findProjectById(projectId);

        // Kiểm tra tên vai trò đã tồn tại trong dự án chưa
        boolean roleExists = project.getRoles().stream()
                .anyMatch(role -> role.getName().equalsIgnoreCase(roleDto.getName()));
        if (roleExists) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "Tên vai trò đã tồn tại trong dự án.");
        }

        // Lấy danh sách permissions từ DB
        Set<Permission> permissions = new HashSet<>(permissionRepository.findAllById(roleDto.getPermissionIds()));
        if (permissions.size() != roleDto.getPermissionIds().size()) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "Một hoặc nhiều quyền hạn không hợp lệ.");
        }

        ProjectRole newRole = ProjectRole.builder()
                .project(project)
                .name(roleDto.getName())
                .description(roleDto.getDescription())
                .permissions(permissions)
                .build();

        ProjectRole savedRole = projectRoleRepository.save(newRole);
        log.info("User '{}' created new role '{}' for project ID {}", getCurrentUser().getEmail(), savedRole.getName(),
                projectId);

        return projectRoleMapper.toProjectRoleResponse(savedRole);
    }

    @Override
    @Transactional
    @PreAuthorize("hasPermission(#projectId, 'project', 'project:role:manage')")
    public ProjectRoleResponse updateProjectRole(Long projectId, Long projectRoleId, ProjectRoleRequest roleDto) {
        Project project = findProjectById(projectId);
        ProjectRole roleToUpdate = project.getRoles().stream()
                .filter(role -> role.getId().equals(projectRoleId))
                .findFirst()
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_REQUEST,
                        "Vai trò không tồn tại hoặc không thuộc dự án này."));

        // Kiểm tra nếu đổi tên thì tên mới có bị trùng không
        if (!roleToUpdate.getName().equalsIgnoreCase(roleDto.getName())) {
            boolean roleExists = project.getRoles().stream()
                    .anyMatch(role -> role.getName().equalsIgnoreCase(roleDto.getName()));
            if (roleExists) {
                throw new ApiException(ErrorCode.INVALID_REQUEST, "Tên vai trò đã tồn tại trong dự án.");
            }
        }

        Set<Permission> permissions = new HashSet<>(permissionRepository.findAllById(roleDto.getPermissionIds()));
        if (permissions.size() != roleDto.getPermissionIds().size()) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "Một hoặc nhiều quyền hạn không hợp lệ.");
        }

        roleToUpdate.setName(roleDto.getName());
        roleToUpdate.setDescription(roleDto.getDescription());
        roleToUpdate.setPermissions(permissions);

        ProjectRole updatedRole = projectRoleRepository.save(roleToUpdate);
        log.info("User '{}' updated role ID {} for project ID {}", getCurrentUser().getEmail(), projectRoleId,
                projectId);

        return projectRoleMapper.toProjectRoleResponse(updatedRole);
    }

    @Override
    @Transactional
    @PreAuthorize("hasPermission(#projectId, 'project', 'project:role:manage')")
    public void deleteProjectRole(Long projectId, Long projectRoleId) {
        Project project = findProjectById(projectId);
        ProjectRole roleToDelete = project.getRoles().stream()
                .filter(role -> role.getId().equals(projectRoleId))
                .findFirst()
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_REQUEST,
                        "Vai trò không tồn tại hoặc không thuộc dự án này."));

        // Validation: Không cho xóa nếu vẫn còn thành viên giữ vai trò này
        boolean isRoleInUse = project.getMembers().stream()
                .anyMatch(member -> member.getProjectRole().getId().equals(projectRoleId));
        if (isRoleInUse) {
            throw new ApiException(ErrorCode.INVALID_REQUEST,
                    "Không thể xóa vai trò đang được sử dụng bởi thành viên.");
        }

        projectRoleRepository.delete(roleToDelete);
        log.warn("User '{}' deleted role ID {} from project ID {}", getCurrentUser().getEmail(), projectRoleId,
                projectId);
    }

    // === Tác vụ nền (Scheduled Task) ===

    @Override
    @Scheduled(cron = "0 0 1 * * ?") // Chạy vào lúc 1 giờ sáng mỗi ngày
    @Transactional
    public void updateExpiredProjectsStatus() {
        log.info("Running scheduled task to update expired projects...");
        // Tìm tất cả các dự án đang ACTIVE (status=1) và có ngày kết thúc đã qua
        List<Project> expiredProjects = projectRepository.findAllByStatusAndEndDateBefore(1, Instant.now());

        if (expiredProjects.isEmpty()) {
            log.info("No expired projects found.");
            return;
        }

        for (Project project : expiredProjects) {
            project.setStatus(3); // 3 = EXPIRED
            projectRepository.save(project);
            log.warn("Project ID {} with end date {} has been marked as EXPIRED.", project.getId(),
                    project.getEndDate());
            // TODO: Gửi email thông báo cho các thành viên và trưởng dự án
        }
        log.info("Finished updating {} expired projects.", expiredProjects.size());
    }

    // --- Helper Methods ---
    private User getCurrentUser() {
        String email = JwtUtils.getCurrentUserLogin().orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND,
                "Không tìm thấy thông tin người dùng trong phiên làm việc."));
        return userRepository.findByEmail(email).orElseThrow(
                () -> new ApiException(ErrorCode.USER_NOT_FOUND, "Người dùng " + email + " không tồn tại."));
    }

    private Project findProjectById(Long projectId) {
        return projectRepository.findById(projectId).orElseThrow(
                () -> new ApiException(ErrorCode.INVALID_REQUEST, "Dự án với ID " + projectId + " không tồn tại."));
    }
}