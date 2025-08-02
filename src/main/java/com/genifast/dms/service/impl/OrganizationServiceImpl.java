package com.genifast.dms.service.impl;

import com.genifast.dms.common.constant.CommonConstants;
import com.genifast.dms.common.constant.ErrorCode;
import com.genifast.dms.common.constant.ErrorMessage;
import com.genifast.dms.common.dto.EmailData;
import com.genifast.dms.dto.request.DeptManagerUpdateRequest;
import com.genifast.dms.dto.request.InviteUsersRequest;
import com.genifast.dms.dto.request.OrganizationCreateRequest;
import com.genifast.dms.dto.request.OrganizationUpdateRequest;
import com.genifast.dms.dto.request.UpdateOrganizationStatusRequest;
import com.genifast.dms.dto.request.UserActionRequest;
import com.genifast.dms.dto.response.CheckOrgResponse;
import com.genifast.dms.dto.response.InviteUsersResponse;
import com.genifast.dms.dto.response.OrganizationResponse;
import com.genifast.dms.dto.response.UserResponse;
import com.genifast.dms.common.exception.ApiException;
import com.genifast.dms.common.service.EmailService;
import com.genifast.dms.common.utils.JwtUtils;
import com.genifast.dms.config.ApplicationProperties;
import com.genifast.dms.entity.Department;
import com.genifast.dms.entity.Organization;
import com.genifast.dms.entity.Permission;
import com.genifast.dms.entity.Role;
import com.genifast.dms.entity.User;
import com.genifast.dms.mapper.OrganizationMapper;
import com.genifast.dms.mapper.UserMapper;
import com.genifast.dms.repository.CategoryRepository;
import com.genifast.dms.repository.DepartmentRepository;
import com.genifast.dms.repository.DocumentRepository;
import com.genifast.dms.repository.OrganizationRepository;
import com.genifast.dms.repository.PermissionRepository;
import com.genifast.dms.repository.RoleRepository;
import com.genifast.dms.repository.UserRepository;
import com.genifast.dms.service.OrganizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrganizationServiceImpl implements OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final DepartmentRepository departmentRepository;
    private final CategoryRepository categoryRepository;
    private final DocumentRepository documentRepository;
    private final OrganizationMapper organizationMapper;
    private final UserMapper userMapper;
    private final EmailService emailService;
    private final ApplicationProperties applicationProperties;

    @Override
    @Transactional
    public OrganizationResponse createOrganization(OrganizationCreateRequest createDto) {
        String email = JwtUtils.getCurrentUserLogin().orElse("");
        User currentUser = findUserByEmail(JwtUtils.getCurrentUserLogin().orElse(""));

        // [cite_start]// 1. Kiểm tra user đã thuộc tổ chức nào chưa [cite: 248]
        if (currentUser.getOrganization() != null) {
            throw new ApiException(ErrorCode.USER_IN_ANOTHER_ORGANIZATION,
                    ErrorMessage.USER_IN_ANOTHER_ORGANIZATION.getMessage());
        }

        // [cite_start]// 2. Kiểm tra tên tổ chức đã tồn tại chưa [cite: 248]
        organizationRepository.findByName(createDto.getName()).ifPresent(org -> {
            throw new ApiException(ErrorCode.ORGANIZATION_ALREADY_EXISTS,
                    ErrorMessage.ORGANIZATION_ALREADY_EXISTS.getMessage());
        });

        // [cite_start]// 3. Kiểm tra user đã gửi yêu cầu nào trước đó chưa [cite: 249]
        organizationRepository.findByCreatedBy(email).ifPresent(org -> {
            throw new ApiException(ErrorCode.USER_REQUEST_CREATE_ORGANIZATION,
                    ErrorMessage.USER_CREATE_ORGANIZATION_REQUESTED.getMessage());
        });

        // 4. Tạo và lưu Organization
        Organization newOrg = new Organization();
        newOrg.setName(createDto.getName());
        newOrg.setDescription(createDto.getDescription());
        newOrg.setIsOpenai(createDto.getIsOpenai());
        newOrg.setStatus(0); // 0 = Pending approval
        newOrg.setLimitData(CommonConstants.LIMIT_DATA);
        newOrg.setDataUsed(0L);

        Organization savedOrg = organizationRepository.save(newOrg);
        log.info("User '{}' created a request for organization '{}' (ID: {})", email, savedOrg.getName(),
                savedOrg.getId());

        return organizationMapper.toOrganizationResponse(savedOrg);
    }

    @Override
    public OrganizationResponse getOrganizationById(Long orgId) {
        User currentUser = findUserByEmail(JwtUtils.getCurrentUserLogin().orElse(""));
        Organization organization = findOrgById(orgId);

        // Kiểm tra quyền truy cập của user
        // checkUserAccessToOrganization(currentUser, orgId);

        return organizationMapper.toOrganizationResponse(organization);
    }

    @Override
    @Transactional
    public OrganizationResponse updateOrganization(Long orgId, OrganizationUpdateRequest updateDto) {
        User currentUser = findUserByEmail(JwtUtils.getCurrentUserLogin().orElse(""));
        Organization organization = findOrgById(orgId);

        if (organization.getStatus() != 1) {
            throw new ApiException(ErrorCode.ORGANIZATION_NOT_EXIST,
                    ErrorMessage.ORGANIZATION_NOT_EXIST.getMessage());
        }

        // checkUserAccessToOrganization(currentUser, orgId);

        // [cite_start]// 1. Kiểm tra quyền: chỉ manager của tổ chức mới được sửa [cite:
        // 253]
        // authorizeUserIsOrgManager(currentUser, orgId);

        // 2. Kiểm tra tên mới có bị trùng với tổ chức khác không
        organizationRepository.findByName(updateDto.getName()).ifPresent(existingOrg -> {
            if (!existingOrg.getId().equals(orgId)) {
                throw new ApiException(ErrorCode.ORGANIZATION_ALREADY_EXISTS,
                        ErrorMessage.ORGANIZATION_ALREADY_EXISTS.getMessage());
            }
        });

        // 3. Cập nhật thông tin
        organization.setName(updateDto.getName());
        organization.setDescription(updateDto.getDescription());

        Organization updatedOrg = organizationRepository.save(organization);
        log.info("Organization ID {} updated by {}", orgId, currentUser.getEmail());

        return organizationMapper.toOrganizationResponse(updatedOrg);
    }

    @Override
    public OrganizationResponse updateOrganizationStatus(Long orgId, UpdateOrganizationStatusRequest statusDto) {
        User currentUser = findUserByEmail(JwtUtils.getCurrentUserLogin().orElse(""));
        // Chỉ Admin hệ thống mới có quyền thay đổi trạng thái tổ chức
        // authorizeUserIsAdmin(currentUser);

        Organization organization = findOrgById(orgId);
        int newStatus = statusDto.getStatus();

        switch (newStatus) {
            case 1: // Approved
                organization.setStatus(1);
                // Tìm người tạo ra tổ chức
                User creator = findUserByEmail(organization.getCreatedBy());

                // Kiểm tra người tạo có đang nằm trong tổ chức khác không
                if (creator.getOrganization() != null) {
                    throw new ApiException(ErrorCode.USER_IN_ANOTHER_ORGANIZATION,
                            ErrorMessage.USER_IN_ANOTHER_ORGANIZATION.getMessage());
                }

                // TẠO ROLE MẶC ĐỊNH CHO TỔ CHỨC MỚI
                createDefaultRolesForOrganization(organization);

                // Gán người tạo làm Manager
                assignManagerRoleToCreator(creator, organization);

                log.info("Organization ID {} has been APPROVED by Admin {}", orgId, currentUser.getEmail());
                break;
            case 2: // Rejected
                organizationRepository.delete(organization);
                log.info("Organization ID {} has been REJECTED and DELETED by Admin {}", orgId, currentUser.getEmail());
                return null; // Trả về null vì đã bị xóa
            case 3: // Suspended
                organization.setStatus(3);
                // Vô hiệu hóa tất cả các phòng ban, danh mục, tài liệu liên quan
                departmentRepository.updateStatusByOrganizationId(orgId, 2); // 2 = Inactive
                categoryRepository.updateStatusByOrganizationId(orgId, 2);
                documentRepository.updateStatusByOrganizationId(orgId, 2);
                log.info("Organization ID {} has been SUSPENDED by Admin {}", orgId, currentUser.getEmail());
                break;
            default:
                throw new ApiException(ErrorCode.INVALID_REQUEST, ErrorMessage.INVALID_REQUEST.getMessage());
        }

        Organization updatedOrg = organizationRepository.save(organization);
        return organizationMapper.toOrganizationResponse(updatedOrg);
    }

    @Override
    public CheckOrgResponse checkUserHasPendingOrganizationRequest() {
        User currentUser = findUserByEmail(JwtUtils.getCurrentUserLogin().orElse(""));

        if (currentUser.getOrganization() != null) {
            throw new ApiException(ErrorCode.USER_IN_ANOTHER_ORGANIZATION,
                    ErrorMessage.USER_IN_ANOTHER_ORGANIZATION.getMessage());
        }

        boolean hasPending = organizationRepository.findByCreatedBy(currentUser.getEmail())
                .map(org -> org.getStatus() == 0) // 0 = Pending
                .orElse(false);

        return new CheckOrgResponse(hasPending);
    }

    @Override
    public InviteUsersResponse inviteUsers(Long orgId, InviteUsersRequest inviteDto) {
        User currentUser = findUserByEmail(JwtUtils.getCurrentUserLogin().orElse(""));
        Organization organization = findOrgById(orgId);

        // 1. Chỉ manager mới có quyền mời
        // authorizeUserIsOrgManager(currentUser, orgId);

        // 2. Lấy danh sách email từ request
        List<String> requestedEmails = inviteDto.getUsers().stream()
                .map(InviteUsersRequest.UserInvite::getEmail)
                .collect(Collectors.toList());

        // 3. Tìm xem email nào đã thuộc một tổ chức khác
        List<String> alreadyInOrgEmails = userRepository.findEmailsOfUsersInOrganization(requestedEmails);

        // 4. Lọc ra danh sách email hợp lệ để mời
        List<InviteUsersRequest.UserInvite> validInvites = inviteDto.getUsers().stream()
                .filter(userInvite -> !alreadyInOrgEmails.contains(userInvite.getEmail()))
                .collect(Collectors.toList());

        // 5. Gửi email mời
        for (InviteUsersRequest.UserInvite invite : validInvites) {
            String joinLink = String.format("%s?orgId=%d&deptId=%d&email=%s",
                    applicationProperties.email().linkJoinOrganization(), orgId, invite.getDepartmentId(),
                    invite.getEmail());

            emailService.sendOrganizationInvitation(invite.getEmail(), new EmailData(organization.getName(), joinLink));
        }

        List<String> invitedEmails = validInvites.stream()
                .map(InviteUsersRequest.UserInvite::getEmail)
                .collect(Collectors.toList());

        log.info("User {} invited {} users to organization ID {}",
                currentUser.getEmail(), invitedEmails.size(), orgId);

        return new InviteUsersResponse(invitedEmails, alreadyInOrgEmails);
    }

    @Override
    @Transactional
    public void acceptInvitation(Long orgId, Long deptId, String userEmail) {
        Organization organization = findOrgById(orgId);
        if (organization.getStatus() != 1) { // 1 = Active
            throw new ApiException(ErrorCode.ORGANIZATION_NOT_EXIST, ErrorMessage.ORGANIZATION_NOT_EXIST.getMessage());
        }

        User user = findUserByEmail(userEmail);
        if (user.getOrganization() != null) {
            throw new ApiException(ErrorCode.USER_IN_ANOTHER_ORGANIZATION,
                    ErrorMessage.USER_IN_ANOTHER_ORGANIZATION.getMessage());
        }

        // Kiểm tra Department có tồn tại và thuộc Organization không
        Department department = departmentRepository.findById(deptId)
                .orElseThrow(
                        () -> new ApiException(ErrorCode.INVALID_REQUEST, "Department not found"));
        if (!department.getOrganization().getId().equals(orgId)) {
            throw new ApiException(ErrorCode.INVALID_REQUEST,
                    "Department does not belong to the specified organization.");
        }

        // Gán role MEMBER cho user
        Role memberRole = roleRepository.findByNameAndOrganizationId("MEMBER", orgId)
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_REQUEST, "MEMBER role not found for this org."));

        user.getRoles().add(memberRole);

        user.setOrganization(organization);
        user.setDepartment(department);
        userRepository.save(user);

        log.info("User {} successfully joined organization '{}' (ID: {}) in department ID {}",
                userEmail, organization.getName(), orgId, deptId);
    }

    @Override
    @Transactional
    public void removeUserFromOrganization(Long orgId, UserActionRequest removeUserDto) {
        User currentUser = findUserByEmail(JwtUtils.getCurrentUserLogin().orElse(""));
        Organization organization = findOrgById(orgId);

        if (organization.getStatus() != 1) { // 1 = Active
            throw new ApiException(ErrorCode.ORGANIZATION_NOT_EXIST, ErrorMessage.ORGANIZATION_NOT_EXIST.getMessage());
        }

        // 1. Chỉ manager mới có quyền xóa thành viên
        // authorizeUserIsOrgManager(currentUser, orgId);

        // 2. Tìm thành viên cần xóa
        User userToRemove = findUserByEmail(removeUserDto.getEmail());

        // 3. Đảm bảo user cần xóa đúng là thành viên của tổ chức này
        if (userToRemove.getOrganization() == null || !userToRemove.getOrganization().getId().equals(orgId)) {
            throw new ApiException(ErrorCode.USER_IN_ANOTHER_ORGANIZATION,
                    ErrorMessage.USER_IN_ANOTHER_ORGANIZATION.getMessage());
        }

        // 4. Quy tắc nghiệp vụ: Không cho phép tự xóa mình hoặc xóa người tạo ra tổ
        // chức
        if (userToRemove.getId().equals(currentUser.getId())) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "Manager cannot remove themselves.");
        }
        if (userToRemove.getEmail().equals(organization.getCreatedBy())) {
            throw new ApiException(ErrorCode.USER_NO_PERMISSION, "Cannot remove the organization creator.");
        }

        // 5. Thực hiện xóa
        userRepository.removeUserFromOrganization(userToRemove.getId());
        log.info("User {} was removed from organization ID {} by manager {}",
                removeUserDto.getEmail(), orgId, currentUser.getEmail());
    }

    @Override
    @Transactional
    public void assignManagerRole(Long orgId, UserActionRequest assignDto) {
        User currentUser = findUserByEmail(JwtUtils.getCurrentUserLogin().orElse(""));
        Organization organization = findOrgById(orgId);

        if (organization.getStatus() != 1) { // 1 = Active
            throw new ApiException(ErrorCode.ORGANIZATION_NOT_EXIST, ErrorMessage.ORGANIZATION_NOT_EXIST.getMessage());
        }

        // 1. Chỉ manager mới có quyền gán quyền
        // authorizeUserIsOrgManager(currentUser, orgId);

        // 2. Tìm user cần gán quyền
        User userToAssign = findUserByEmail(assignDto.getEmail());
        if (userToAssign.getOrganization() == null || !userToAssign.getOrganization().getId().equals(orgId)) {
            throw new ApiException(ErrorCode.USER_NOT_IN_ORGANIZATION,
                    ErrorMessage.USER_NOT_IN_ORGANIZATION.getMessage());
        }

        if (userToAssign.getStatus() != 1) {
            throw new ApiException(ErrorCode.USER_INACTIVE, ErrorMessage.USER_INACTIVE.getMessage());
        }

        // 3. Gán quyền và lưu
        Set<Role> roles = getDefaultRolesForOrganization(organization);

        userToAssign.getRoles().addAll(roles);
        userToAssign.setIsOrganizationManager(true);
        userRepository.save(userToAssign);
        log.info("Manager role assigned to user {} in organization ID {} by manager {}",
                assignDto.getEmail(), orgId, currentUser.getEmail());
    }

    @Override
    @Transactional
    public void recallManagerRole(Long orgId, UserActionRequest recallDto) {
        User currentUser = findUserByEmail(JwtUtils.getCurrentUserLogin().orElse(""));
        Organization organization = findOrgById(orgId);

        if (organization.getStatus() != 1) { // 1 = Active
            throw new ApiException(ErrorCode.ORGANIZATION_NOT_EXIST, ErrorMessage.ORGANIZATION_NOT_EXIST.getMessage());
        }

        // 1. Logic đặc biệt: Chỉ người tạo tổ chức mới có quyền bãi nhiệm
        authorizeUserIsOrgCreator(currentUser, organization);

        // 2. Tìm manager cần bãi nhiệm
        User managerToRecall = findUserByEmail(recallDto.getEmail());
        if (managerToRecall.getOrganization() == null || !managerToRecall.getOrganization().getId().equals(orgId)) {
            throw new ApiException(ErrorCode.USER_NOT_IN_ORGANIZATION,
                    ErrorMessage.USER_NOT_IN_ORGANIZATION.getMessage());
        }

        if (managerToRecall.getStatus() != 1) {
            throw new ApiException(ErrorCode.USER_INACTIVE, ErrorMessage.USER_INACTIVE.getMessage());
        }

        // 3. Không thể bãi nhiệm chính người tạo
        if (managerToRecall.getEmail().equals(organization.getCreatedBy())) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "Cannot recall the organization creator.");
        }

        // 4. Bãi nhiệm và lưu
        managerToRecall.getRoles().removeIf(role -> "ORGANIZATION_MANAGER".equals(role.getName()));
        if (!managerToRecall.getIsDeptManager())
            managerToRecall.getRoles().removeIf(role -> "DEPARTMENT_MANAGER".equals(role.getName()));

        managerToRecall.setIsOrganizationManager(false);
        userRepository.save(managerToRecall);
        log.info("Manager role recalled from user {} in organization ID {} by creator {}",
                recallDto.getEmail(), orgId, currentUser.getEmail());
    }

    @Override
    public Page<UserResponse> getOrganizationMembers(Long orgId, Pageable pageable) {
        User currentUser = findUserByEmail(JwtUtils.getCurrentUserLogin().orElse(""));

        // Authorize: User phải là thành viên của tổ chức
        // checkUserAccessToOrganization(currentUser, orgId);

        Page<User> userPage = userRepository.findByOrganizationId(orgId, pageable);

        return userPage.map(userMapper::toUserResponse);
    }

    @Override
    public Page<UserResponse> getDepartmentMembers(Long deptId, Pageable pageable) {
        User currentUser = findUserByEmail(JwtUtils.getCurrentUserLogin().orElse(""));
        Department department = findDepartmentById(deptId);

        // Authorize: User phải là thành viên của tổ chức chứa phòng ban này
        // checkUserAccessToOrganization(currentUser,
        // department.getOrganization().getId());

        // Cho phép cả Org Manager và Dept Manager xem thành viên
        // authorizeUserIsOrgManagerOrDeptManager(currentUser);

        Page<User> userPage = userRepository.findByDepartmentId(deptId, pageable);

        return userPage.map(userMapper::toUserResponse);
    }

    @Override
    @Transactional
    public void updateDepartmentManagerRole(Long orgId, DeptManagerUpdateRequest deptManagerUpdateRequest) {
        User currentUser = findUserByEmail(JwtUtils.getCurrentUserLogin().orElse(""));

        // 1. Authorize: Chỉ manager của tổ chức mới được quyền gán/bãi nhiệm
        // authorizeUserIsOrgManager(currentUser, orgId);

        // 2. Tìm user mục tiêu
        User targetUser = findUserByEmail(deptManagerUpdateRequest.getEmail());

        // 3. Validation:
        // a. Đảm bảo user mục tiêu thuộc cùng tổ chức
        checkUserAccessToOrganization(targetUser, orgId);
        // b. Đảm bảo user mục tiêu phải thuộc một phòng ban
        if (targetUser.getDepartment() == null) {
            throw new ApiException(ErrorCode.USER_NOT_IN_DEPARTMENT, ErrorMessage.USER_NOT_IN_DEPARTMENT.getMessage());
        }
        // c. Manager không thể tự thay đổi vai trò của chính mình
        if (targetUser.getId().equals(currentUser.getId())) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "Cannot change your own role.");
        }

        // 4. Cập nhật vai trò
        Role deptManagerRole = roleRepository.findByNameAndOrganizationId("DEPARTMENT_MANAGER", orgId)
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_REQUEST,
                        "DEPARTMENT_MANAGER role not found for this org."));

        if (deptManagerUpdateRequest.getIsManager()) {
            targetUser.getRoles().add(deptManagerRole);
        } else {
            targetUser.getRoles().remove(deptManagerRole);
        }

        userRepository.updateUserDeptManagerRole(targetUser.getId(), deptManagerUpdateRequest.getIsManager());

        String action = deptManagerUpdateRequest.getIsManager() ? "assigned" : "recalled";
        log.info("Department manager role {} for user {} by manager {}",
                action, targetUser.getEmail(), currentUser.getEmail());
    }

    // --- Helper Methods ---

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND, ErrorMessage.INVALID_USER.getMessage()));
    }

    private Organization findOrgById(Long orgId) {
        return organizationRepository.findById(orgId)
                .orElseThrow(
                        () -> new ApiException(ErrorCode.ORGANIZATION_NOT_EXIST,
                                ErrorMessage.ORGANIZATION_NOT_EXIST.getMessage()));
    }

    private Department findDepartmentById(Long deptId) {
        return departmentRepository.findById(deptId)
                .orElseThrow(() -> new ApiException(ErrorCode.DEPARTMENT_NOT_FOUND,
                        ErrorMessage.DEPARTMENT_NOT_FOUND.getMessage()));
    }

    private void checkUserAccessToOrganization(User user, Long orgId) {
        if (user.getOrganization() == null || !user.getOrganization().getId().equals(orgId)) {
            throw new ApiException(ErrorCode.CANNOT_ACCESS_ORGANIZATION,
                    ErrorMessage.CANNOT_ACCESS_ORGANIZATION.getMessage());
        }
    }

    private void authorizeUserIsAdmin(User user) {
        if (!user.getIsAdmin()) {
            throw new ApiException(ErrorCode.USER_NO_PERMISSION, ErrorMessage.NO_PERMISSION.getMessage());
        }
    }

    private void authorizeUserIsOrgManager(User user, Long orgId) {
        if (user.getIsOrganizationManager() == null || !user.getIsOrganizationManager()) {
            throw new ApiException(ErrorCode.USER_NO_PERMISSION, ErrorMessage.NO_PERMISSION.getMessage());
        }
    }

    private void authorizeUserIsOrgCreator(User user, Organization organization) {
        if (!user.getEmail().equals(organization.getCreatedBy())) {
            throw new ApiException(ErrorCode.USER_NO_PERMISSION,
                    "This action requires organization creator privileges.");
        }
    }

    private void authorizeUserIsOrgManagerOrDeptManager(User user) {
        boolean isOrgManager = user.getIsOrganizationManager() != null && user.getIsOrganizationManager();
        boolean isDeptManager = user.getIsDeptManager() != null && user.getIsDeptManager();

        if (!isOrgManager && !isDeptManager) {
            throw new ApiException(ErrorCode.USER_NO_PERMISSION, "User must be an organization or department manager.");
        }
    }

    private void createDefaultRolesForOrganization(Organization org) {
        // Role MEMBER
        if (roleRepository.findByNameAndOrganizationId("MEMBER", org.getId()).isEmpty()) {
            Set<Permission> memberPermissions = new HashSet<>(permissionRepository.findAllByNameIn(
                    Arrays.asList("documents:read", "documents:create", "documents:update", "documents:comment",
                            "documents:submit", "organization:view-details", "organization:view-members",
                            "department:view-details", "department:view-list-by-org", "department:view-categories",
                            "category:view-documents")));
            Role memberRole = Role.builder().name("MEMBER").organization(org).permissions(memberPermissions).build();
            roleRepository.save(memberRole);
        }

        // Role DEPARTMENT_MANAGER
        if (roleRepository.findByNameAndOrganizationId("DEPARTMENT_MANAGER", org.getId()).isEmpty()) {
            Set<Permission> deptManagerPermissions = new HashSet<>(permissionRepository.findAllByNameIn(
                    Arrays.asList("documents:read", "documents:create", "documents:update", "documents:comment",
                            "documents:submit", "department:create", "department:update", "department:view-details",
                            "category:create", "category:update", "category:view-details", "category:update-status")));
            Role deptManagerRole = Role.builder().name("DEPARTMENT_MANAGER").organization(org)
                    .permissions(deptManagerPermissions).build();
            roleRepository.save(deptManagerRole);
        }

        // Role ORGANIZATION_MANAGER
        if (roleRepository.findByNameAndOrganizationId("ORGANIZATION_MANAGER", org.getId()).isEmpty()) {
            Set<Permission> orgManagerPermissions = new HashSet<>(permissionRepository.findAllByNameIn(
                    Arrays.asList("documents:read", "documents:create", "documents:update", "documents:delete",
                            "documents:approve", "documents:reject", "organization:update", "organization:invite-users",
                            "organization:remove-user", "organization:assign-manager", "organization:recall-manager",
                            "department:update-status", "department:update-manager")));
            Role orgManagerRole = Role.builder().name("ORGANIZATION_MANAGER").organization(org)
                    .permissions(orgManagerPermissions).build();
            roleRepository.save(orgManagerRole);
        }
    }

    private void assignManagerRoleToCreator(User creator, Organization organization) {
        Set<Role> defaultRoles = getDefaultRolesForOrganization(organization);

        // Gán role cho người tạo
        creator.getRoles().addAll(defaultRoles);
        creator.setOrganization(organization);
        creator.setIsOrganizationManager(true);
        userRepository.save(creator);
    }

    private Set<Role> getDefaultRolesForOrganization(Organization organization) {
        Role orgManagerRole = roleRepository.findByNameAndOrganizationId("ORGANIZATION_MANAGER", organization.getId())
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_REQUEST,
                        "ORGANIZATION_MANAGER role not found for this org."));
        Role deptManagerRole = roleRepository.findByNameAndOrganizationId("DEPARTMENT_MANAGER", organization.getId())
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_REQUEST,
                        "DEPARTMENT_MANAGER role not found for this org."));
        Role memberRole = roleRepository.findByNameAndOrganizationId("MEMBER", organization.getId())
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_REQUEST, "MEMBER role not found for this org."));

        return new HashSet<>(Arrays.asList(orgManagerRole, deptManagerRole, memberRole));
    }
}