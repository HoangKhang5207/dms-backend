package com.genifast.dms.service.impl;

import com.genifast.dms.common.exception.ApiException;
import com.genifast.dms.common.constant.ErrorCode;
import com.genifast.dms.common.constant.ErrorMessage;
import com.genifast.dms.dto.request.RoleRequestDto;
import com.genifast.dms.dto.response.RoleResponseDto;
import com.genifast.dms.entity.Organization;
import com.genifast.dms.entity.Permission;
import com.genifast.dms.entity.Role;
import com.genifast.dms.entity.User;
import com.genifast.dms.mapper.RoleMapper;
import com.genifast.dms.repository.OrganizationRepository;
import com.genifast.dms.repository.PermissionRepository;
import com.genifast.dms.repository.RoleRepository;
import com.genifast.dms.repository.UserRepository;
import com.genifast.dms.service.RoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoleServiceImpl implements RoleService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final OrganizationRepository organizationRepository;
    private final RoleMapper roleMapper;

    /**
     * Tạo mới một Role.
     * Hỗ trợ tạo Role cho một Organization cụ thể (nếu organizationId được cung
     * cấp)
     * hoặc tạo Role cấp hệ thống (nếu organizationId là null).
     */
    @Override
    @Transactional
    public RoleResponseDto createRole(RoleRequestDto roleDto) {
        Organization organization = null;

        // --- Logic xử lý Organization ---
        if (roleDto.getOrganizationId() != null) {
            // Trường hợp 1: Tạo Role cho một Organization
            organization = organizationRepository.findById(roleDto.getOrganizationId())
                    .orElseThrow(() -> new ApiException(ErrorCode.ORGANIZATION_NOT_EXIST,
                            ErrorMessage.ORGANIZATION_NOT_EXIST.getMessage()));

            // Kiểm tra tên Role trùng lặp trong Organization
            if (roleRepository.findByNameAndOrganizationId(roleDto.getName(), organization.getId()).isPresent()) {
                throw new ApiException(ErrorCode.INVALID_REQUEST, "Vai trò đã tồn tại trong tổ chức này.");
            }
        } else {
            // Trường hợp 2: Tạo Role cấp hệ thống
            // Kiểm tra tên Role trùng lặp trong các role hệ thống
            if (roleRepository.findByNameAndOrganizationIdIsNull(roleDto.getName()).isPresent()) {
                throw new ApiException(ErrorCode.INVALID_REQUEST, "Vai trò đã tồn tại trong hệ thống.");
            }
        }

        Role role = roleMapper.toRole(roleDto);
        role.setOrganization(organization); // Gán organization (có thể là null)

        // --- Logic gán Permissions ---
        if (!CollectionUtils.isEmpty(roleDto.getPermissionIds())) {
            List<Permission> permissions = permissionRepository.findAllById(roleDto.getPermissionIds());
            if (permissions.size() != roleDto.getPermissionIds().size()) {
                throw new ApiException(ErrorCode.INVALID_REQUEST, "Một hoặc nhiều quyền không tồn tại.");
            }
            role.setPermissions(new HashSet<>(permissions));
        }

        Role savedRole = roleRepository.save(role);
        return roleMapper.toRoleResponseDto(savedRole);
    }

    /**
     * Cập nhật một Role đã có.
     * Đảm bảo kiểm tra tính duy nhất của tên Role trong phạm vi (hệ thống hoặc tổ
     * chức).
     * Không cho phép thay đổi Organization của một Role.
     */
    @Override
    @Transactional
    public RoleResponseDto updateRole(Long roleId, RoleRequestDto roleDto) {
        Role existingRole = roleRepository.findById(roleId)
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_REQUEST, "Vai trò không tồn tại."));

        // --- Logic kiểm tra tên trùng lặp khi cập nhật ---
        if (!existingRole.getName().equalsIgnoreCase(roleDto.getName())) {
            Optional<Role> duplicateRole;
            if (existingRole.getOrganization() != null) {
                // Role thuộc về một Organization
                duplicateRole = roleRepository.findByNameAndOrganizationId(roleDto.getName(),
                        existingRole.getOrganization().getId());
            } else {
                // Role hệ thống
                duplicateRole = roleRepository.findByNameAndOrganizationIdIsNull(roleDto.getName());
            }

            if (duplicateRole.isPresent()) {
                throw new ApiException(ErrorCode.INVALID_REQUEST, "Tên vai trò đã tồn tại.");
            }
            existingRole.setName(roleDto.getName());
        }

        existingRole.setDescription(roleDto.getDescription());

        // --- Logic cập nhật Permissions ---
        if (roleDto.getPermissionIds() != null) {
            existingRole.getPermissions().clear(); // Xóa các quyền cũ
            if (!roleDto.getPermissionIds().isEmpty()) {
                List<Permission> newPermissions = permissionRepository.findAllById(roleDto.getPermissionIds());
                if (newPermissions.size() != roleDto.getPermissionIds().size()) {
                    throw new ApiException(ErrorCode.INVALID_REQUEST, "Một hoặc nhiều quyền không tồn tại.");
                }
                existingRole.setPermissions(new HashSet<>(newPermissions)); // Gán các quyền mới
            }
        }

        Role updatedRole = roleRepository.save(existingRole);
        return roleMapper.toRoleResponseDto(updatedRole);
    }

    @Override
    @Transactional
    public void deleteRole(Long roleId) {
        Role roleToDelete = roleRepository.findById(roleId)
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_REQUEST, "Vai trò không tồn tại."));

        // Bước 1: Tìm tất cả user đang có role này
        List<User> usersWithRole = userRepository.findByRoles_Id(roleId);

        // Bước 2: Xóa role này khỏi danh sách roles của mỗi user
        for (User user : usersWithRole) {
            user.getRoles().remove(roleToDelete);
            userRepository.save(user); // Cập nhật lại user
        }

        // Bước 3: Sau khi đã xóa hết các mối liên kết, giờ mới có thể xóa role
        roleRepository.delete(roleToDelete);
        log.warn("Role ID {} and all its user associations have been deleted.", roleId);
    }

    @Override
    public List<RoleResponseDto> getAllRolesByOrg(Long orgId) {
        if (!organizationRepository.existsById(orgId)) {
            throw new ApiException(ErrorCode.ORGANIZATION_NOT_EXIST, ErrorMessage.ORGANIZATION_NOT_EXIST.getMessage());
        }
        List<Role> roles = roleRepository.findByOrganizationId(orgId);
        return roleMapper.toRoleResponseDtoList(roles);
    }

    @Override
    public RoleResponseDto getRoleById(Long roleId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_REQUEST, "Vai trò không tồn tại."));
        return roleMapper.toRoleResponseDto(role);
    }
}