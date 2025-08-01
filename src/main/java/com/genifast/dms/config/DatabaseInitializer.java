package com.genifast.dms.config;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.genifast.dms.entity.Permission;
import com.genifast.dms.entity.Role;
import com.genifast.dms.entity.User;
import com.genifast.dms.repository.PermissionRepository;
import com.genifast.dms.repository.RoleRepository;
import com.genifast.dms.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class DatabaseInitializer implements CommandLineRunner {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        log.info(">>> STARTING DATABASE INITIALIZATION <<<");
        long countUsers = this.userRepository.count();
        long countRoles = this.roleRepository.count();
        long countPermissions = this.permissionRepository.count();

        Map<String, Permission> allPermissions = new HashMap<>();
        if (countPermissions == 0) {
            allPermissions = createPermissions();
        }

        if (countRoles == 0) {
            createSystemRoles(allPermissions);
        }

        if (countUsers == 0) {
            createAdminUser();
        }

        if (countUsers > 0 && countRoles > 0 && countPermissions > 0) {
            log.info(">>> DATABASE INITIALIZATION SKIPPED <<<");

        } else {
            log.info(">>> DATABASE INITIALIZATION COMPLETED <<<");
        }
    }

    private void createAdminUser() {
        if (userRepository.findByEmail("admin@gmail.com").isEmpty()) {
            User adminUser = new User();
            adminUser.setEmail("admin@gmail.com");
            adminUser.setPassword(this.passwordEncoder.encode("123456"));
            adminUser.setFirstName("Admin");
            adminUser.setLastName("System");
            adminUser.setGender(false);
            adminUser.setStatus(1); // Active
            adminUser.setIsAdmin(true); // Giữ lại flag này để tương thích

            // Gán role SYSTEM_ADMIN
            Role adminRole = roleRepository.findByNameAndOrganizationIdIsNull("SYSTEM_ADMIN")
                    .orElseThrow(() -> new RuntimeException("SYSTEM_ADMIN role not found!"));
            adminUser.setRoles(Set.of(adminRole));

            userRepository.save(adminUser);
            log.info(">>> Created default admin user 'admin@gmail.com' <<<");
        }
    }

    private Map<String, Permission> createPermissions() {
        List<String> permissionNames = Arrays.asList(
                "documents:read", "documents:create", "documents:update", "documents:delete",
                "documents:approve", "documents:reject", "documents:download", "documents:upload",
                "documents:sign", "documents:lock", "documents:unlock", "documents:comment",
                "documents:history", "documents:archive", "documents:restore", "documents:share:readonly",
                "documents:share:forwardable", "documents:share:timebound", "documents:share:external",
                "documents:share:orgscope", "documents:submit", "documents:distribute", "documents:publish",
                "documents:track", "documents:version:read", "documents:notify", "documents:report",
                "documents:export", "documents:forward", "delegate_process", "audit:log",
                // Thêm các quyền quản trị
                "organization:update-status", "organization:view-list", "user:manage", "role:manage",

                // Các quyền cho DEFAULT_USER
                "user:view-profile", "user:update-profile", "user:change-password",
                "organization:create-request",

                // Các quyền cho ORGANIZATION_MANAGER, DEPARTMENT_MANAGER, MEMBER
                "organization:view-details", "organization:update", "organization:invite-users",
                "organization:remove-user", "organization:assign-manager", "organization:recall-manager",
                "organization:view-members",
                "department:create", "department:update", "department:view-details",
                "department:view-list-by-org", "department:update-status", "department:update-manager",
                "department:view-members",
                "department:view-categories",
                "category:create", "category:update", "category:view-details", "category:update-status",
                "category:view-documents");

        Map<String, Permission> existingPermissions = permissionRepository.findAll().stream()
                .collect(Collectors.toMap(Permission::getName, Function.identity()));

        for (String name : permissionNames) {
            if (!existingPermissions.containsKey(name)) {
                Permission newPermission = Permission.builder().name(name).description("Permission for " + name)
                        .build();
                permissionRepository.save(newPermission);
                existingPermissions.put(name, newPermission);
            }
        }
        log.info(">>> Ensured all {} permissions exist <<<", permissionNames.size());
        return existingPermissions;
    }

    private void createSystemRoles(Map<String, Permission> permissions) {
        // --- ROLE: SYSTEM_ADMIN ---
        if (roleRepository.findByNameAndOrganizationIdIsNull("SYSTEM_ADMIN").isEmpty()) {
            Set<Permission> adminPermissions = new HashSet<>(permissions.values()); // Admin có tất cả quyền
            Role adminRole = Role.builder()
                    .name("SYSTEM_ADMIN")
                    .description("Super administrator with all permissions")
                    .isInheritable(false)
                    .organization(null) // Role hệ thống
                    .permissions(adminPermissions)
                    .build();
            roleRepository.save(adminRole);
            log.info(">>> Created SYSTEM_ADMIN role <<<");
        }

        // --- ROLE: DEFAULT_USER (cho người dùng mới đăng ký) ---
        if (roleRepository.findByNameAndOrganizationIdIsNull("DEFAULT_USER").isEmpty()) {
            Set<Permission> defaultPermissions = Set.of(
                    permissions.get("user:view-profile"),
                    permissions.get("user:update-profile"),
                    permissions.get("user:change-password"),
                    permissions.get("organization:create-request"));
            Role defaultRole = Role.builder()
                    .name("DEFAULT_USER")
                    .description("Default role for newly registered users before joining an organization")
                    .isInheritable(false)
                    .organization(null)
                    .permissions(defaultPermissions)
                    .build();
            roleRepository.save(defaultRole);
            log.info(">>> Created DEFAULT_USER role <<<");
        }

        // Các role thuộc về organization (như ORGANIZATION_MANAGER) sẽ được tạo khi tạo
        // Organization
        // Hoặc có thể tạo sẵn mẫu ở đây với organization_id = null để clone sau.
        // Tạm thời chúng ta sẽ tạo khi cần.
    }

}
