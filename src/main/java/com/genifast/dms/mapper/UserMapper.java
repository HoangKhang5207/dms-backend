package com.genifast.dms.mapper;

import com.genifast.dms.dto.request.SignUpRequestDto;
import com.genifast.dms.dto.response.UserResponse;
import com.genifast.dms.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", // Tích hợp với Spring DI
        unmappedTargetPolicy = ReportingPolicy.IGNORE // Bỏ qua các trường không được map
)
public abstract class UserMapper {

    @org.springframework.beans.factory.annotation.Autowired
    protected com.genifast.dms.repository.PermissionRepository permissionRepository;

    /**
     * Chuyển đổi từ SignUpRequestDto sang User entity.
     * Tự động map các trường có tên giống nhau.
     * Bỏ qua trường 'password' vì chúng ta sẽ xử lý mã hóa riêng.
     */
    @Mapping(target = "password", ignore = true)
    public abstract User toUser(SignUpRequestDto signUpRequestDto);

    @Mapping(source = "department.id", target = "departmentId")
    @Mapping(source = "organization.id", target = "organizationId")
    @Mapping(source = "fullName", target = "fullName")
    @Mapping(source = "department.name", target = "departmentName")
    @Mapping(source = "position.name", target = "positionName")
    @Mapping(target = "roleNames", expression = "java(mapRoleNames(user))")
    @Mapping(target = "permissionNames", expression = "java(mapPermissionNames(user))")
    public abstract UserResponse toUserResponse(User user);

    protected java.util.Set<String> mapRoleNames(User user) {
        if (user.getRoles() == null)
            return new java.util.HashSet<>();
        return user.getRoles().stream()
                .map(com.genifast.dms.entity.Role::getName)
                .collect(java.util.stream.Collectors.toSet());
    }

    protected java.util.Set<String> mapPermissionNames(User user) {
        java.util.Set<String> permissions = new java.util.HashSet<>();

        // 1. Get permissions from Roles
        if (user.getRoles() != null) {
            user.getRoles().forEach(role -> {
                if (role.getPermissions() != null) {
                    role.getPermissions().forEach(p -> permissions.add(p.getName()));
                }
            });
        }

        // 2. Get permissions from UserPermissions (Direct assignment)
        if (user.getUserPermissions() != null) {
            user.getUserPermissions().forEach(up -> {
                if ("GRANT".equalsIgnoreCase(up.getAction()) && up.getPermission() != null) {
                    permissions.add(up.getPermission().getName());
                }
                // Handle "DENY" if needed logic requires it, but for now we list available
                // permissions.
                // If "DENY" is supported, we should remove from set.
                // Assuming simple additive model for display or explicit "GRANT".
            });
        }

        return permissions;
    }
}