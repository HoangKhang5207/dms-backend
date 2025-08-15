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
        if (userRepository.findByEmail("quantri@genifast.edu.vn").isEmpty()) {
            User adminUser = new User();
            adminUser.setEmail("quantri@genifast.edu.vn");
            adminUser.setPassword(this.passwordEncoder.encode("123456"));
            adminUser.setFirstName("H");
            adminUser.setLastName("Lê Thị");
            adminUser.setGender(false);
            adminUser.setStatus(1); // Active
            adminUser.setIsAdmin(true); // Giữ lại flag này để tương thích

            // Gán role Quản trị viên
            Role adminRole = roleRepository.findByNameAndOrganizationIdIsNull("SYSTEM_ADMIN")
                    .orElseThrow(() -> new RuntimeException("vai trò SYSTEM_ADMIN không tồn tại!"));
            adminUser.setRoles(Set.of(adminRole));

            userRepository.save(adminUser);
            log.info(">>> Created default admin user 'quantri@genifast.edu.vn' <<<");
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

                // --- BỔ SUNG CÁC QUYỀN MỚI CHO DỰ ÁN ---
                "project:read", // Quyền xem thông tin dự án
                "project:manage", // Quyền quản lý chung (sửa, xóa dự án)
                "project:member:manage", // Quyền quản lý thành viên (thêm, xóa, đổi vai trò)
                "project:role:manage" // Quyền quản lý vai trò tùy chỉnh trong dự án
        );

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
        // --- ROLE: Quản trị viên ---
        if (roleRepository.findByNameAndOrganizationIdIsNull("SYSTEM_ADMIN").isEmpty()) {
            Set<Permission> adminPermissions = new HashSet<>(permissions.values()); // Admin có tất cả quyền
            Role adminRole = Role.builder()
                    .name("SYSTEM_ADMIN")
                    .description("Quản trị viên - Quản lý toàn hệ thống, có quyền xem và quản lý log.")
                    .isInheritable(false)
                    .organization(null) // Role hệ thống
                    .permissions(adminPermissions)
                    .build();
            roleRepository.save(adminRole);
            log.info(">>> Đã tạo vai trò Quản trị viên <<<");
        }
    }

}
