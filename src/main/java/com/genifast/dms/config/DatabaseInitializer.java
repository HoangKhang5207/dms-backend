package com.genifast.dms.config;

import java.time.Instant;
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

import com.genifast.dms.entity.*;
import com.genifast.dms.entity.enums.DocumentConfidentiality;
import com.genifast.dms.entity.enums.DocumentStatus;
import com.genifast.dms.entity.enums.DeviceType;
import com.genifast.dms.repository.*;

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
    private final OrganizationRepository organizationRepository;
    private final DepartmentRepository departmentRepository;
    private final CategoryRepository categoryRepository;
    private final DocumentRepository documentRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectRoleRepository projectRoleRepository;
    private final DeviceRepository deviceRepository;
    private final UserPermissionRepository userPermissionRepository;
    private final PrivateDocRepository privateDocRepository;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        log.info(">>> STARTING DATABASE INITIALIZATION <<<");
        
        // Check existing data
        long countUsers = this.userRepository.count();
        long countRoles = this.roleRepository.count();
        long countPermissions = this.permissionRepository.count();
        long countOrganizations = this.organizationRepository.count();
        long countDepartments = this.departmentRepository.count();

        // Create permissions first
        Map<String, Permission> allPermissions = new HashMap<>();
        if (countPermissions == 0) {
            allPermissions = createPermissions();
        }

        // Create system roles
        if (countRoles == 0) {
            createSystemRoles(allPermissions);
        }

        // Create test organizations and departments
        Organization testOrg = null;
        if (countOrganizations == 0) {
            testOrg = createTestOrganizations();
        } else {
            testOrg = organizationRepository.findById(1L).orElse(null);
        }

        // Create departments
        Map<String, Department> departments = new HashMap<>();
        if (countDepartments == 0 && testOrg != null) {
            departments = createTestDepartments(testOrg);
        }

        // Create admin and test users
        if (countUsers == 0) {
            createAdminUser();
            if (testOrg != null && !departments.isEmpty()) {
                // Create org roles and assign permissions per scenario
                createOrganizationRolesWithPermissions(testOrg);
                // Create users per scenario
                createTestUsers(testOrg, departments);
            }
        }

        // Create test projects and documents
        if (testOrg != null && !departments.isEmpty()) {
            createTestProjectsAndDocuments(testOrg, departments);
        }

        log.info(">>> DATABASE INITIALIZATION COMPLETED <<<");
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
            Role adminRole = roleRepository.findByNameAndOrganizationIsNull("Quản trị viên")
                    .orElseThrow(() -> new RuntimeException("vai trò Quản trị viên role không tồn tại!"));
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
                Permission newPermission = Permission.builder().name(name).description("Permission for " + name).createdBy("system")
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
        if (roleRepository.findByNameAndOrganizationIsNull("Quản trị viên").isEmpty()) {
            Set<Permission> adminPermissions = new HashSet<>(permissions.values()); // Admin có tất cả quyền
            Role adminRole = Role.builder()
                    .name("Quản trị viên")
                    .description("Quản lý toàn hệ thống, có quyền xem và quản lý log.")
                    .isInheritable(false)
                    .organization(null) // Role hệ thống
                    .permissions(adminPermissions)
                    .build();
            roleRepository.save(adminRole);
            log.info(">>> Đã tạo vai trò Quản trị viên <<<");
        }
    }

    private Organization createTestOrganizations() {
        Organization organization = Organization.builder()
                .name("Đại học Genifast")
                .description("Trường đại học công nghệ hàng đầu Việt Nam")
                .status(1) // Active
                .createdAt(java.time.LocalDateTime.now())
                .createdBy("system")
                .build();
        organizationRepository.save(organization);
        log.info(">>> Đã tạo tổ chức test: {} <<<", organization.getName());
        return organization;
    }

    private Map<String, Department> createTestDepartments(Organization organization) {
        Map<String, Department> departments = new HashMap<>();
        
        Department deptCNTT = Department.builder()
                .name("Khoa Công nghệ Thông tin")
                .description("Phụ trách đào tạo và nghiên cứu về CNTT")
                .organization(organization)
                .status(1)
                .createdAt(java.time.LocalDateTime.now())
                .createdBy("system")
                .build();
        departmentRepository.save(deptCNTT);
        departments.put("K.CNTT", deptCNTT);

        Department deptDaoTao = Department.builder()
                .name("Phòng Đào tạo")
                .description("Quản lý chương trình và kết quả học tập")
                .organization(organization)
                .status(1)
                .createdAt(java.time.LocalDateTime.now())
                .createdBy("system")
                .build();
        departmentRepository.save(deptDaoTao);
        departments.put("P.DTAO", deptDaoTao);

        Department deptTCHC = Department.builder()
                .name("Phòng Tổ chức Hành chính")
                .description("Quản lý nhân sự, hành chính, và văn thư")
                .organization(organization)
                .status(1)
                .createdAt(java.time.LocalDateTime.now())
                .createdBy("system")
                .build();
        departmentRepository.save(deptTCHC);
        departments.put("P.TCHC", deptTCHC);

        Department deptBGH = Department.builder()
                .name("Ban Giám hiệu")
                .description("Lãnh đạo và điều hành toàn bộ hoạt động của trường")
                .organization(organization)
                .status(1)
                .createdAt(java.time.LocalDateTime.now())
                .createdBy("system")
                .build();
        departmentRepository.save(deptBGH);
        departments.put("BGH", deptBGH);

        Department deptBGHPC = Department.builder()
                .name("Bộ phận Pháp chế")
                .description("Trực thuộc BGH, phụ trách kiểm tra pháp lý và tư vấn pháp lý")
                .organization(organization)
                .status(1)
                .createdAt(java.time.LocalDateTime.now())
                .createdBy("system")
                .build();
        departmentRepository.save(deptBGHPC);
        departments.put("BGH.PC", deptBGHPC);

        Department deptLT = Department.builder()
                .name("Phòng Lưu trữ")
                .description("Quản lý lưu trữ tài liệu")
                .organization(organization)
                .status(1)
                .createdAt(java.time.LocalDateTime.now())
                .createdBy("system")
                .build();
        departmentRepository.save(deptLT);
        departments.put("P.LT", deptLT);
        
        log.info(">>> Đã tạo {} phòng ban test (K.CNTT, P.DTAO, P.TCHC, BGH, BGH.PC, P.LT) <<<", departments.size());
        return departments;
    }

    private Permission getPermission(String name) {
        return permissionRepository.findByName(name)
                .orElseThrow(() -> new RuntimeException("Permission not found: " + name));
    }

    private void createOrganizationRolesWithPermissions(Organization org) {
        // Helper to create role with a set of permissions
        java.util.function.BiConsumer<String, List<String>> ensureRole = (roleName, perms) -> {
            if (roleRepository.findByNameAndOrganizationIsNull(roleName).isPresent()) {
                return; // Skip system-level role name collision
            }
            roleRepository.findByNameAndOrganization_Id(roleName, org.getId()).orElseGet(() -> {
                Set<Permission> p = perms.stream().map(this::getPermission).collect(Collectors.toSet());
                Role r = Role.builder()
                        .name(roleName)
                        .description("Role for organization: " + org.getName())
                        .organization(org)
                        .isInheritable(false)
                        .permissions(p)
                        .build();
                return roleRepository.save(r);
            });
        };

        // Permissions per matrix (core ones). Adjusted to names present in system
        List<String> allRead = List.of("documents:read", "documents:download", "documents:version:read");
        List<String> basicEdit = List.of("documents:create", "documents:update", "documents:delete", "documents:upload", "documents:submit", "documents:comment");
        List<String> approveReject = List.of("documents:approve", "documents:reject");
        List<String> adminExtra = List.of("documents:sign", "documents:lock", "documents:unlock", "documents:history", "documents:archive", "documents:restore", "documents:publish", "documents:track", "documents:notify", "documents:report", "documents:export", "audit:log");
        List<String> sharePerms = List.of("documents:share:readonly", "documents:share:forwardable", "documents:share:timebound", "documents:share:orgscope");

        ensureRole.accept("Hiệu trưởng",
                concat(allRead, basicEdit, approveReject, adminExtra, sharePerms));
        ensureRole.accept("Trưởng khoa", concat(allRead, basicEdit, approveReject, sharePerms));
        ensureRole.accept("Phó khoa", concat(allRead, basicEdit));
        ensureRole.accept("Chuyên viên", concat(allRead, basicEdit));
        ensureRole.accept("Giáo vụ", concat(allRead, basicEdit));
        ensureRole.accept("Phó phòng", concat(allRead, basicEdit));
        ensureRole.accept("Cán bộ", concat(allRead));
        ensureRole.accept("Văn thư", concat(allRead));
        ensureRole.accept("Pháp chế", concat(allRead, List.of("documents:approve", "documents:reject")));
        ensureRole.accept("Nhân viên Lưu trữ", concat(allRead, List.of("documents:archive", "documents:restore")));
        ensureRole.accept("Người nhận", concat(allRead));
        // Quản trị viên hệ thống đã được tạo ở mức system
    }

    @SafeVarargs
    private static List<String> concat(List<String>... lists) {
        return Arrays.stream(lists).flatMap(List::stream).distinct().collect(Collectors.toList());
    }

    private void createTestUsers(Organization organization, Map<String, Department> departments) {
        // Helper to attach role by name
        java.util.function.BiConsumer<User, String> attachRole = (u, roleName) -> {
            Role r = roleRepository.findByNameAndOrganization_Id(roleName, organization.getId())
                    .orElseGet(() -> roleRepository.findByNameAndOrganizationIsNull(roleName).orElse(null));
            if (r != null) {
                u.getRoles().add(r);
            }
        };

        // user-ht: Hiệu trưởng (admin=true, is_organization_manager=true)
        User userHt = User.builder()
                .firstName("Nguyễn")
                .lastName("Văn A")
                .fullName("Nguyễn Văn A")
                .email("hieutruong@genifast.edu.vn")
                .password(passwordEncoder.encode("123456"))
                .gender(true)
                .status(1)
                .isAdmin(true)
                .isOrganizationManager(true)
                .organization(organization)
                .department(departments.get("BGH"))
                .isDeptManager(false)
                .createdAt(java.time.LocalDateTime.now())
                .updatedAt(java.time.LocalDateTime.now())
                .build();
        attachRole.accept(userHt, "Hiệu trưởng");
        userRepository.save(userHt);

        // user-tk: Trưởng khoa (is_dept_manager=true)
        User userTk = User.builder()
                .firstName("Trần Thị")
                .lastName("B")
                .fullName("Trần Thị B")
                .email("truongkhoa.cntt@genifast.edu.vn")
                .password(passwordEncoder.encode("123456"))
                .gender(false)
                .status(1)
                .isAdmin(false)
                .isOrganizationManager(false)
                .organization(organization)
                .department(departments.get("K.CNTT"))
                .isDeptManager(true)
                .createdAt(java.time.LocalDateTime.now())
                .updatedAt(java.time.LocalDateTime.now())
                .build();
        attachRole.accept(userTk, "Trưởng khoa");
        userRepository.save(userTk);

        // user-pk: Phó khoa
        User userPk = User.builder()
                .firstName("Lê Văn")
                .lastName("C")
                .fullName("Lê Văn C")
                .email("phokhoa.cntt@genifast.edu.vn")
                .password(passwordEncoder.encode("123456"))
                .gender(true)
                .status(1)
                .isAdmin(false)
                .isOrganizationManager(false)
                .organization(organization)
                .department(departments.get("K.CNTT"))
                .isDeptManager(false)
                .createdAt(java.time.LocalDateTime.now())
                .updatedAt(java.time.LocalDateTime.now())
                .build();
        attachRole.accept(userPk, "Phó khoa");
        userRepository.save(userPk);

        // user-cv: Chuyên viên (P.DTAO)
        User userCv = User.builder()
                .firstName("Phạm Thị")
                .lastName("D")
                .fullName("Phạm Thị D")
                .email("chuyenvien.dtao@genifast.edu.vn")
                .password(passwordEncoder.encode("123456"))
                .gender(false)
                .status(1)
                .isAdmin(false)
                .isOrganizationManager(false)
                .organization(organization)
                .department(departments.get("P.DTAO"))
                .isDeptManager(false)
                .createdAt(java.time.LocalDateTime.now())
                .updatedAt(java.time.LocalDateTime.now())
                .build();
        attachRole.accept(userCv, "Chuyên viên");
        userRepository.save(userCv);

        // Gán quyền chia sẻ user-specific theo Kịch bản 1 cho user-cv
        try {
            Permission pReadonly = permissionRepository.findByName("documents:share:readonly")
                    .orElseThrow(() -> new RuntimeException("Permission 'documents:share:readonly' không tồn tại"));
            Permission pOrgScope = permissionRepository.findByName("documents:share:orgscope")
                    .orElseThrow(() -> new RuntimeException("Permission 'documents:share:orgscope' không tồn tại"));
            Permission pTimebound = permissionRepository.findByName("documents:share:timebound")
                    .orElseThrow(() -> new RuntimeException("Permission 'documents:share:timebound' không tồn tại"));
            userPermissionRepository.save(UserPermission.builder()
                    .user(userCv)
                    .permission(pReadonly)
                    .action("GRANT")
                    .build());
            userPermissionRepository.save(UserPermission.builder()
                    .user(userCv)
                    .permission(pOrgScope)
                    .action("GRANT")
                    .build());
            userPermissionRepository.save(UserPermission.builder()
                    .user(userCv)
                    .permission(pTimebound)
                    .action("GRANT")
                    .build());
            log.info(">>> Gán quyền chia sẻ readonly, orgscope & timebound cho user-cv <<<");
        } catch (Exception e) {
            log.warn("Không thể gán quyền chia sẻ user-specific cho user-cv: {}", e.getMessage());
        }

        // user-gv: Giáo vụ (K.CNTT)
        User userGv = User.builder()
                .firstName("Đỗ Văn")
                .lastName("E")
                .fullName("Đỗ Văn E")
                .email("giaovu.cntt@genifast.edu.vn")
                .password(passwordEncoder.encode("123456"))
                .gender(true)
                .status(1)
                .isAdmin(false)
                .isOrganizationManager(false)
                .organization(organization)
                .department(departments.get("K.CNTT"))
                .isDeptManager(false)
                .createdAt(java.time.LocalDateTime.now())
                .updatedAt(java.time.LocalDateTime.now())
                .build();
        attachRole.accept(userGv, "Giáo vụ");
        userRepository.save(userGv);

        // user-pp: Phó phòng (P.DTAO, is_dept_manager=true)
        User userPp = User.builder()
                .firstName("Nguyễn Thị")
                .lastName("F")
                .fullName("Nguyễn Thị F")
                .email("phophong.dtao@genifast.edu.vn")
                .password(passwordEncoder.encode("123456"))
                .gender(false)
                .status(1)
                .isAdmin(false)
                .isOrganizationManager(false)
                .organization(organization)
                .department(departments.get("P.DTAO"))
                .isDeptManager(true)
                .createdAt(java.time.LocalDateTime.now())
                .updatedAt(java.time.LocalDateTime.now())
                .build();
        attachRole.accept(userPp, "Phó phòng");
        userRepository.save(userPp);

        // user-cb: Cán bộ (P.TCHC)
        User userCb = User.builder()
                .firstName("Trần Văn")
                .lastName("G")
                .fullName("Trần Văn G")
                .email("canbo.tchc@genifast.edu.vn")
                .password(passwordEncoder.encode("123456"))
                .gender(true)
                .status(1)
                .isAdmin(false)
                .isOrganizationManager(false)
                .organization(organization)
                .department(departments.get("P.TCHC"))
                .isDeptManager(false)
                .createdAt(java.time.LocalDateTime.now())
                .updatedAt(java.time.LocalDateTime.now())
                .build();
        attachRole.accept(userCb, "Cán bộ");
        userRepository.save(userCb);

        // user-vt: Văn thư (P.TCHC)
        User userVt = User.builder()
                .firstName("Nguyễn Thị")
                .lastName("Văn")
                .fullName("Nguyễn Thị Văn")
                .email("vanthu.tchc@genifast.edu.vn")
                .password(passwordEncoder.encode("123456"))
                .gender(false)
                .status(1)
                .isAdmin(false)
                .isOrganizationManager(false)
                .organization(organization)
                .department(departments.get("P.TCHC"))
                .isDeptManager(false)
                .createdAt(java.time.LocalDateTime.now())
                .updatedAt(java.time.LocalDateTime.now())
                .build();
        attachRole.accept(userVt, "Văn thư");
        userRepository.save(userVt);

        // user-pc: Pháp chế (BGH.PC, is_dept_manager=true)
        User userPc = User.builder()
                .firstName("Lê Thị")
                .lastName("Pháp")
                .fullName("Lê Thị Pháp")
                .email("phapche.bgh@genifast.edu.vn")
                .password(passwordEncoder.encode("123456"))
                .gender(false)
                .status(1)
                .isAdmin(false)
                .isOrganizationManager(false)
                .organization(organization)
                .department(departments.get("BGH.PC"))
                .isDeptManager(true)
                .createdAt(java.time.LocalDateTime.now())
                .updatedAt(java.time.LocalDateTime.now())
                .build();
        attachRole.accept(userPc, "Pháp chế");
        userRepository.save(userPc);

        // user-lt: Nhân viên Lưu trữ (P.LT)
        User userLt = User.builder()
                .firstName("Nguyễn Thị")
                .lastName("F")
                .fullName("Nguyễn Thị F")
                .email("luutru@genifast.edu.vn")
                .password(passwordEncoder.encode("123456"))
                .gender(false)
                .status(1)
                .isAdmin(false)
                .isOrganizationManager(false)
                .organization(organization)
                .department(departments.get("P.LT"))
                .isDeptManager(false)
                .createdAt(java.time.LocalDateTime.now())
                .updatedAt(java.time.LocalDateTime.now())
                .build();
        attachRole.accept(userLt, "Nhân viên Lưu trữ");
        userRepository.save(userLt);

        // user-nn: Người nhận (K.CNTT)
        User userNn = User.builder()
                .firstName("Trần Văn")
                .lastName("G")
                .fullName("Trần Văn G")
                .email("nguoinhan@genifast.edu.vn")
                .password(passwordEncoder.encode("123456"))
                .gender(true)
                .status(1)
                .isAdmin(false)
                .isOrganizationManager(false)
                .organization(organization)
                .department(departments.get("K.CNTT"))
                .isDeptManager(false)
                .createdAt(java.time.LocalDateTime.now())
                .updatedAt(java.time.LocalDateTime.now())
                .build();
        attachRole.accept(userNn, "Người nhận");
        userRepository.save(userNn);

        // user-qtv: Quản trị viên (BGH) - giữ lại vai trò hệ thống và is_admin=true
        if (userRepository.findByEmail("quantri@genifast.edu.vn").isEmpty()) {
            User userQtv = User.builder()
                    .firstName("Lê Thị")
                    .lastName("H")
                    .fullName("Lê Thị H")
                    .email("quantri@genifast.edu.vn")
                    .password(passwordEncoder.encode("123456"))
                    .gender(false)
                    .status(1)
                    .isAdmin(true)
                    .isOrganizationManager(false)
                    .organization(organization)
                    .department(departments.get("BGH"))
                    .isDeptManager(false)
                    .createdAt(java.time.LocalDateTime.now())
                    .updatedAt(java.time.LocalDateTime.now())
                    .build();
            userRepository.save(userQtv);
        }

        // user-ext: External user (no org)
        User userExt = User.builder()
                .firstName("Hoàng Văn")
                .lastName("H")
                .fullName("Hoàng Văn H")
                .email("external@other.org")
                .password(passwordEncoder.encode("123456"))
                .gender(true)
                .status(1)
                .isAdmin(false)
                .isOrganizationManager(false)
                .organization(null)
                .department(null)
                .isDeptManager(false)
                .createdAt(java.time.LocalDateTime.now())
                .updatedAt(java.time.LocalDateTime.now())
                .build();
        userRepository.save(userExt);

        // user-inactive
        User userInactive = User.builder()
                .firstName("Vũ Thị")
                .lastName("I")
                .fullName("Vũ Thị I")
                .email("inactive@genifast.edu.vn")
                .password(passwordEncoder.encode("123456"))
                .gender(false)
                .status(2)
                .isAdmin(false)
                .isOrganizationManager(false)
                .organization(organization)
                .department(departments.get("P.TCHC"))
                .isDeptManager(false)
                .createdAt(java.time.LocalDateTime.now())
                .updatedAt(java.time.LocalDateTime.now())
                .build();
        userRepository.save(userInactive);

        // user-visitor
        User userVisitor = User.builder()
                .firstName("Guest")
                .lastName("Visitor")
                .fullName("Guest Visitor")
                .email("visitor@external.com")
                .password(passwordEncoder.encode("123456"))
                .gender(false)
                .status(1)
                .isAdmin(false)
                .isOrganizationManager(false)
                .organization(null)
                .department(null)
                .isDeptManager(false)
                .createdAt(java.time.LocalDateTime.now())
                .updatedAt(java.time.LocalDateTime.now())
                .build();
        userRepository.save(userVisitor);

        log.info(">>> Đã tạo người dùng test theo kịch bản <<<");
    }

    private void createTestProjectsAndDocuments(Organization organization, Map<String, Department> departments) {
        // Project for doc-proj-01
        Project project = Project.builder()
                .name("Dự án DMS Giai đoạn 2")
                .description("Triển khai chi tiết DMS GĐ2")
                .startDate(Instant.now().minusSeconds(7L * 24 * 3600))
                .endDate(Instant.now().plusSeconds(90L * 24 * 3600))
                .status(1)
                .organization(organization)
                .build();
        projectRepository.save(project);

        // Ensure default ProjectRole entities for this project
        java.util.function.Function<String, com.genifast.dms.entity.ProjectRole> ensureProjectRole = name ->
                projectRoleRepository.findByProject_IdAndName(project.getId(), name).orElseGet(() -> {
                    com.genifast.dms.entity.ProjectRole pr = com.genifast.dms.entity.ProjectRole.builder()
                            .project(project)
                            .name(name)
                            .description(name + " role")
                            .build();
                    return projectRoleRepository.save(pr);
                });

        com.genifast.dms.entity.ProjectRole managerRole = ensureProjectRole.apply("PROJECT_MANAGER");
        com.genifast.dms.entity.ProjectRole memberRole = ensureProjectRole.apply("MEMBER");

        // Project members: user-tk (manager), user-cv (member)
        userRepository.findByEmail("truongkhoa.cntt@genifast.edu.vn").ifPresent(u -> {
            ProjectMember pm = ProjectMember.builder()
                    .project(project)
                    .user(u)
                    .projectRole(managerRole)
                    .build();
            projectMemberRepository.save(pm);
        });
        userRepository.findByEmail("chuyenvien.dtao@genifast.edu.vn").ifPresent(u -> {
            ProjectMember pm = ProjectMember.builder()
                    .project(project)
                    .user(u)
                    .projectRole(memberRole)
                    .build();
            projectMemberRepository.save(pm);
        });

        // Categories per documents
        Category catQuyChe = Category.builder()
                .name("Quy chế")
                .description("Quy chế")
                .organization(organization)
                .department(departments.get("P.DTAO"))
                .status(1)
                .createdAt(Instant.now())
                .build();
        categoryRepository.save(catQuyChe);

        Category catKeHoachDaoTao = Category.builder()
                .name("Kế hoạch đào tạo")
                .description("Kế hoạch đào tạo")
                .organization(organization)
                .department(departments.get("P.DTAO"))
                .status(1)
                .createdAt(Instant.now())
                .build();
        categoryRepository.save(catKeHoachDaoTao);

        Category catHopTacQT = Category.builder()
                .name("Hợp tác quốc tế")
                .description("Hợp tác quốc tế")
                .organization(organization)
                .department(departments.get("BGH"))
                .status(1)
                .createdAt(Instant.now())
                .build();
        categoryRepository.save(catHopTacQT);

        Category catBaoCaoTC = Category.builder()
                .name("Báo cáo tài chính")
                .description("Báo cáo tài chính")
                .organization(organization)
                .department(departments.get("P.TCHC"))
                .status(1)
                .createdAt(Instant.now())
                .build();
        categoryRepository.save(catBaoCaoTC);

        Category catHopDong = Category.builder()
                .name("Hợp đồng")
                .description("Hợp đồng")
                .organization(organization)
                .department(departments.get("BGH"))
                .status(1)
                .createdAt(Instant.now())
                .build();
        categoryRepository.save(catHopDong);

        Category catDanhSach = Category.builder()
                .name("Danh sách")
                .description("Danh sách")
                .organization(organization)
                .department(departments.get("P.DTAO"))
                .status(1)
                .createdAt(Instant.now())
                .build();
        categoryRepository.save(catDanhSach);

        // Helper to build recipients by emails
        java.util.function.Function<List<String>, String> recipientIdsJson = emails -> {
            List<Long> ids = emails.stream()
                    .map(e -> userRepository.findByEmail(e).map(User::getId).orElse(null))
                    .filter(java.util.Objects::nonNull)
                    .collect(Collectors.toList());
            return ids.toString();
        };

        // doc-01: INTERNAL + PENDING, P.DTAO, Quy chế, recipients [user-ht, user-cv]
        Document doc01 = Document.builder()
                .title("Quy chế tuyển sinh 2026")
                .content("Dummy content")
                .type("vanban")
                .description("Quy chế tuyển sinh 2026")
                .organization(organization)
                .department(departments.get("P.DTAO"))
                .category(catQuyChe)
                .confidentiality(DocumentConfidentiality.INTERNAL.getValue())
                .status(DocumentStatus.PENDING.getValue())
                .versionNumber(1)
                .recipients(recipientIdsJson.apply(List.of("hieutruong@genifast.edu.vn", "chuyenvien.dtao@genifast.edu.vn")))
                .createdBy("chuyenvien.dtao@genifast.edu.vn")
                .createdAt(Instant.now())
                .build();
        documentRepository.save(doc01);

        // doc-02: PRIVATE + APPROVED, K.CNTT, Kế hoạch đào tạo, recipients [user-tk, user-gv]
        Document doc02 = Document.builder()
                .title("Kế hoạch đào tạo ngành CNTT")
                .content("Dummy content")
                .type("vanban")
                .description("Kế hoạch đào tạo ngành CNTT")
                .organization(organization)
                .department(departments.get("K.CNTT"))
                .category(catKeHoachDaoTao)
                .confidentiality(DocumentConfidentiality.PRIVATE.getValue())
                .accessType(4) // PRIVATE access type
                .status(DocumentStatus.APPROVED.getValue())
                .versionNumber(1)
                .recipients(recipientIdsJson.apply(List.of("truongkhoa.cntt@genifast.edu.vn", "giaovu.cntt@genifast.edu.vn")))
                .createdBy("giaovu.cntt@genifast.edu.vn")
                .createdAt(Instant.now())
                .build();
        documentRepository.save(doc02);

        // doc-03: INTERNAL + APPROVED (BGH), Hợp tác quốc tế, recipients [user-tk, user-pk]
        Document doc03 = Document.builder()
                .title("Kế hoạch hợp tác quốc tế 2025")
                .content("Dummy content")
                .type("vanban")
                .description("Kế hoạch hợp tác quốc tế 2025")
                .organization(organization)
                .department(departments.get("BGH"))
                .category(catHopTacQT)
                .confidentiality(DocumentConfidentiality.INTERNAL.getValue())
                .status(DocumentStatus.APPROVED.getValue())
                .versionNumber(1)
                .recipients(recipientIdsJson.apply(List.of("truongkhoa.cntt@genifast.edu.vn", "phokhoa.cntt@genifast.edu.vn")))
                .createdBy("hieutruong@genifast.edu.vn")
                .createdAt(Instant.now())
                .build();
        documentRepository.save(doc03);

        // doc-04: LOCKED + APPROVED (P.TCHC), Báo cáo tài chính, recipients [user-ht, user-lt]
        Document doc04 = Document.builder()
                .title("Báo cáo tài chính 2025")
                .content("Dummy content")
                .type("vanban")
                .description("Báo cáo tài chính 2025")
                .organization(organization)
                .department(departments.get("P.TCHC"))
                .category(catBaoCaoTC)
                .confidentiality(DocumentConfidentiality.LOCKED.getValue())
                .status(DocumentStatus.APPROVED.getValue())
                .versionNumber(1)
                .recipients(recipientIdsJson.apply(List.of("hieutruong@genifast.edu.vn", "luutru@genifast.edu.vn")))
                .createdBy("canbo.tchc@genifast.edu.vn")
                .createdAt(Instant.now())
                .build();
        documentRepository.save(doc04);

        // doc-05: EXTERNAL (kịch bản)
        Document doc05 = Document.builder()
                .title("Hợp đồng đào tạo liên kết")
                .content("Dummy content")
                .type("vanban")
                .description("Hợp đồng đào tạo liên kết")
                .organization(organization)
                .department(departments.get("BGH"))
                .category(catHopDong)
                .confidentiality(DocumentConfidentiality.EXTERNAL.getValue())
                .status(DocumentStatus.APPROVED.getValue())
                .versionNumber(1)
                .createdBy("hieutruong@genifast.edu.vn")
                .createdAt(Instant.now())
                .build();
        documentRepository.save(doc05);

        // doc-06: PUBLIC + APPROVED (P.DTAO), Kế hoạch đào tạo
        Document doc06 = Document.builder()
                .title("Kế hoạch đào tạo 2025")
                .content("Dummy content")
                .type("vanban")
                .description("Kế hoạch đào tạo 2025")
                .organization(organization)
                .department(departments.get("P.DTAO"))
                .category(catKeHoachDaoTao)
                .confidentiality(DocumentConfidentiality.PUBLIC.getValue())
                .status(DocumentStatus.APPROVED.getValue())
                .versionNumber(1)
                .createdBy("phophong.dtao@genifast.edu.vn")
                .createdAt(Instant.now())
                .build();
        documentRepository.save(doc06);

        // doc-07: PRIVATE + APPROVED (P.DTAO), Danh sách, recipients [user-pp]
        Document doc07 = Document.builder()
                .title("Danh sách sinh viên")
                .content("Dummy content")
                .type("vanban")
                .description("Danh sách sinh viên")
                .organization(organization)
                .department(departments.get("P.DTAO"))
                .category(catDanhSach)
                .confidentiality(DocumentConfidentiality.PRIVATE.getValue())
                .accessType(4) // PRIVATE access type
                .status(DocumentStatus.APPROVED.getValue())
                .versionNumber(1)
                .recipients(recipientIdsJson.apply(List.of("phophong.dtao@genifast.edu.vn")))
                .createdBy("chuyenvien.dtao@genifast.edu.vn")
                .createdAt(Instant.now())
                .build();
        documentRepository.save(doc07);

        // Seed private_docs: cho phép user-pp truy cập doc-07 (PRIVATE)
        userRepository.findByEmail("phophong.dtao@genifast.edu.vn").ifPresent(upp -> {
            try {
                PrivateDoc pd = PrivateDoc.builder()
                        .document(doc07)
                        .user(upp)
                        .status(1)
                        .build();
                privateDocRepository.save(pd);
                log.info(">>> Seed private_docs: user-pp được phép truy cập doc-07 <<<");
            } catch (Exception ex) {
                log.warn("Không thể seed private_docs cho doc-07 & user-pp: {}", ex.getMessage());
            }
        });

        // Seed private_docs: cho phép creator và recipients truy cập doc-02 (PRIVATE)
        userRepository.findByEmail("giaovu.cntt@genifast.edu.vn").ifPresent(creator -> {
            try {
                PrivateDoc pd = PrivateDoc.builder()
                        .document(doc02)
                        .user(creator)
                        .status(1)
                        .build();
                privateDocRepository.save(pd);
                log.info(">>> Seed private_docs: creator có thể truy cập doc-02 <<<");
            } catch (Exception ex) {
                log.warn("Không thể seed private_docs cho doc-02 & creator: {}", ex.getMessage());
            }
        });
        userRepository.findByEmail("truongkhoa.cntt@genifast.edu.vn").ifPresent(recipient -> {
            try {
                PrivateDoc pd = PrivateDoc.builder()
                        .document(doc02)
                        .user(recipient)
                        .status(1)
                        .build();
                privateDocRepository.save(pd);
                log.info(">>> Seed private_docs: user-tk có thể truy cập doc-02 <<<");
            } catch (Exception ex) {
                log.warn("Không thể seed private_docs cho doc-02 & user-tk: {}", ex.getMessage());
            }
        });

        // doc-08: INTERNAL + DRAFT (P.DTAO)
        Document doc08 = Document.builder()
                .title("Bản nháp kế hoạch 2026")
                .content("Dummy content")
                .type("vanban")
                .description("Bản nháp kế hoạch 2026")
                .organization(organization)
                .department(departments.get("P.DTAO"))
                .category(catKeHoachDaoTao)
                .confidentiality(DocumentConfidentiality.INTERNAL.getValue())
                .status(DocumentStatus.DRAFT.getValue())
                .versionNumber(1)
                .createdBy("chuyenvien.dtao@genifast.edu.vn")
                .createdAt(Instant.now())
                .build();
        documentRepository.save(doc08);

        // doc-09: PUBLIC + PENDING (P.DTAO)
        Document doc09 = Document.builder()
                .title("Danh sách học bổng")
                .content("Dummy content")
                .type("vanban")
                .description("Danh sách học bổng")
                .organization(organization)
                .department(departments.get("P.DTAO"))
                .category(catDanhSach)
                .confidentiality(DocumentConfidentiality.PUBLIC.getValue())
                .status(DocumentStatus.PENDING.getValue())
                .versionNumber(1)
                .createdBy("phophong.dtao@genifast.edu.vn")
                .createdAt(Instant.now())
                .build();
        documentRepository.save(doc09);

        // doc-proj-01: PROJECT + PENDING (K.CNTT), recipients [user-tk, user-cv]
        Document docProj01 = Document.builder()
                .title("Kế hoạch chi tiết triển khai DMS GĐ2")
                .content("Dummy content")
                .type("vanban")
                .description("Kế hoạch chi tiết triển khai DMS GĐ2")
                .organization(organization)
                .department(departments.get("K.CNTT"))
                .project(project)
                .confidentiality(DocumentConfidentiality.PROJECT.getValue())
                .status(DocumentStatus.PENDING.getValue())
                .versionNumber(1)
                .recipients(recipientIdsJson.apply(List.of("truongkhoa.cntt@genifast.edu.vn", "chuyenvien.dtao@genifast.edu.vn")))
                .createdBy("truongkhoa.cntt@genifast.edu.vn")
                .createdAt(Instant.now())
                .build();
        documentRepository.save(docProj01);

        // Devices: one company device for user-cv, one external for visitor
        Device companyDevice = Device.builder()
                .deviceName("Laptop Dell Inspiron 15")
                .deviceType(DeviceType.COMPANY_DEVICE)
                .user(userRepository.findByEmail("chuyenvien.dtao@genifast.edu.vn").orElse(null))
                .status(1)
                .registeredAt(Instant.now())
                .build();
        deviceRepository.save(companyDevice);

        Device externalDevice = Device.builder()
                .deviceName("iPhone 14 Pro")
                .deviceType(DeviceType.EXTERNAL_DEVICE)
                .user(userRepository.findByEmail("visitor@external.com").orElse(null))
                .status(1)
                .registeredAt(Instant.now())
                .build();
        deviceRepository.save(externalDevice);

        log.info(">>> Đã tạo dự án, vai trò dự án, danh mục và tài liệu theo kịch bản <<<");
    }

}
