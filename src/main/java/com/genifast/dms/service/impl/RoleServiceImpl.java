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

@Service
@RequiredArgsConstructor
@Slf4j
public class RoleServiceImpl implements RoleService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final OrganizationRepository organizationRepository;
    private final RoleMapper roleMapper;

    @Override
    @Transactional
    public RoleResponseDto createRole(RoleRequestDto roleDto) {
        // 1. Kiểm tra Organization tồn tại
        Organization org = organizationRepository.findById(roleDto.getOrganizationId())
                .orElseThrow(() -> new ApiException(ErrorCode.ORGANIZATION_NOT_EXIST, "Organization not found."));

        // 2. Kiểm tra tên Role trùng lặp trong Organization
        if (roleRepository.findByNameAndOrganizationId(roleDto.getName(), org.getId()).isPresent()) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "Role name already exists in this organization.");
        }

        Role role = roleMapper.toRole(roleDto);
        role.setOrganization(org);

        // 3. Gán Permissions cho Role
        if (!CollectionUtils.isEmpty(roleDto.getPermissionIds())) {
            List<Permission> permissions = permissionRepository.findAllById(roleDto.getPermissionIds());
            role.setPermissions(new HashSet<>(permissions));
        }

        Role savedRole = roleRepository.save(role);
        return roleMapper.toRoleResponseDto(savedRole);
    }

    @Override
    @Transactional
    public RoleResponseDto updateRole(Long roleId, RoleRequestDto roleDto) {
        Role existingRole = roleRepository.findById(roleId)
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_REQUEST, "Role not found."));

        // Cập nhật các thuộc tính cơ bản
        existingRole.setName(roleDto.getName());
        existingRole.setDescription(roleDto.getDescription());

        // Cập nhật danh sách permissions
        if (roleDto.getPermissionIds() != null) {
            existingRole.getPermissions().clear(); // Xóa các quyền cũ
            if (!roleDto.getPermissionIds().isEmpty()) {
                List<Permission> newPermissions = permissionRepository.findAllById(roleDto.getPermissionIds());
                existingRole.setPermissions(new HashSet<>(newPermissions)); // Thêm các quyền mới
            }
        }

        Role updatedRole = roleRepository.save(existingRole);
        return roleMapper.toRoleResponseDto(updatedRole);
    }

    @Override
    @Transactional
    public void deleteRole(Long roleId) {
        Role roleToDelete = roleRepository.findById(roleId)
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_REQUEST, "Role not found."));

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
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_REQUEST, "Role not found."));
        return roleMapper.toRoleResponseDto(role);
    }
}