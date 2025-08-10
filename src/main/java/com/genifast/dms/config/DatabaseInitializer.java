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
    private final DeviceRepository deviceRepository;

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
            Role adminRole = roleRepository.findByNameAndOrganizationIdIsNull("Quản trị viên")
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
        if (roleRepository.findByNameAndOrganizationIdIsNull("Quản trị viên").isEmpty()) {
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
                .description("Khoa đào tạo về CNTT và Khoa học máy tính")
                .organization(organization)
                .status(1) // Active
                .createdAt(java.time.LocalDateTime.now())
                .createdBy("system")
                .build();
        departmentRepository.save(deptCNTT);
        
        Department deptDaoTao = Department.builder()
                .name("Phòng Đào tạo")
                .description("Phòng quản lý công tác đào tạo")
                .organization(organization)
                .status(1) // Active
                .createdAt(java.time.LocalDateTime.now())
                .createdBy("system")
                .build();
        departmentRepository.save(deptDaoTao);
        departments.put("DTAO", deptDaoTao);
        
        Department deptBGH = Department.builder()
                .name("Ban Giám hiệu")
                .description("Ban lãnh đạo trường đại học")
                .organization(organization)
                .status(1) // Active
                .createdAt(java.time.LocalDateTime.now())
                .createdBy("system")
                .build();
        departmentRepository.save(deptBGH);
        departments.put("BGH", deptBGH);
        departments.put("CNTT", deptCNTT);
        
        log.info(">>> Đã tạo {} phòng ban test <<<", departments.size());
        return departments;
    }

    private void createTestUsers(Organization organization, Map<String, Department> departments) {
        User hieuTruong = User.builder()
                .firstName("Nguyễn")
                .lastName("Văn Hiệu")
                .fullName("Nguyễn Văn Hiệu")
                .email("hieutruong@genifast.edu.vn")
                .password(passwordEncoder.encode("password123"))
                .gender(true) // Male
                .status(1) // Active
                .isAdmin(false)
                .organization(organization)
                .department(departments.get("BGH"))
                .isDeptManager(true)
                .createdAt(java.time.LocalDateTime.now())
                .updatedAt(java.time.LocalDateTime.now())
                .build();
        userRepository.save(hieuTruong);
        
        User truongKhoa = User.builder()
                .firstName("Trần")
                .lastName("Thị Minh")
                .fullName("Trần Thị Minh")
                .email("truongkhoa.cntt@genifast.edu.vn")
                .password(passwordEncoder.encode("password123"))
                .gender(false) // Female
                .status(1) // Active
                .isAdmin(false)
                .organization(organization)
                .department(departments.get("CNTT"))
                .isDeptManager(true)
                .createdAt(java.time.LocalDateTime.now())
                .updatedAt(java.time.LocalDateTime.now())
                .build();
        userRepository.save(truongKhoa);
        
        User chuyenVien = User.builder()
                .firstName("Lê")
                .lastName("Văn Chuyên")
                .fullName("Lê Văn Chuyên")
                .email("chuyenvien.dtao@genifast.edu.vn")
                .password(passwordEncoder.encode("password123"))
                .gender(true) // Male
                .status(1) // Active
                .isAdmin(false)
                .organization(organization)
                .department(departments.get("DTAO"))
                .isDeptManager(false)
                .createdAt(java.time.LocalDateTime.now())
                .updatedAt(java.time.LocalDateTime.now())
                .build();
        userRepository.save(chuyenVien);
        
        User visitor = User.builder()
                .firstName("Phạm")
                .lastName("Thị Khách")
                .fullName("Phạm Thị Khách")
                .email("visitor@external.com")
                .password(passwordEncoder.encode("password123"))
                .gender(false) // Female
                .status(1) // Active
                .isAdmin(false)
                .organization(null) // External user
                .department(null)
                .isDeptManager(false)
                .createdAt(java.time.LocalDateTime.now())
                .updatedAt(java.time.LocalDateTime.now())
                .build();
        userRepository.save(visitor);
        
        log.info(">>> Đã tạo 4 user test <<<");
    }

    private void createTestProjectsAndDocuments(Organization organization, Map<String, Department> departments) {
        Project project = Project.builder()
                .name("Dự án Phát triển Hệ thống DMS")
                .description("Dự án xây dựng hệ thống quản lý tài liệu điện tử")
                .startDate(Instant.now().minusSeconds(30 * 24 * 3600)) // 30 days ago
                .endDate(Instant.now().plusSeconds(180 * 24 * 3600)) // 180 days from now
                .status(1) // ACTIVE
                .organization(organization)
                .build();
        projectRepository.save(project);
        
        Category quyCheCat = Category.builder()
                .name("Quy chế - Quy định")
                .description("Các văn bản quy chế, quy định của trường")
                .organization(organization)
                .department(departments.get("DTAO"))
                .status(1) // Active
                .createdAt(Instant.now())
                .build();
        categoryRepository.save(quyCheCat);
        
        Category danhSachCat = Category.builder()
                .name("Danh sách sinh viên")
                .description("Các danh sách liên quan đến sinh viên")
                .organization(organization)
                .department(departments.get("DTAO"))
                .status(1) // Active
                .createdAt(Instant.now())
                .build();
        categoryRepository.save(danhSachCat);
        
        // Tạo documents test
        // Document 1: Internal document
        Document internalDoc = Document.builder()
                .title("Quy chế tuyển sinh 2026")
                .content("Dummy content")
                .type("vanban")
                .description("Quy chế tuyển sinh đại học năm 2026")
                .organization(organization)
                .department(departments.get("DTAO"))
                .category(quyCheCat)
                .confidentiality(DocumentConfidentiality.INTERNAL.getValue())
                .status(2) // PENDING
                .versionNumber(1)
                .createdBy("chuyenvien.dtao@genifast.edu.vn")
                .createdAt(Instant.now())
                .build();
        documentRepository.save(internalDoc);
        
        // Document 2: Private document
        Document privateDoc = Document.builder()
                .title("Danh sách sinh viên khóa 2024")
                .content("Dummy content")
                .type("vanban")
                .description("Danh sách chi tiết sinh viên khóa tuyển 2024")
                .organization(organization)
                .department(departments.get("DTAO"))
                .category(danhSachCat)
                .confidentiality(DocumentConfidentiality.PRIVATE.getValue())
                .status(1) // APPROVED
                .versionNumber(1)
                .recipients("[4]") // JSON array - Chỉ chuyên viên được xem
                .createdBy("chuyenvien.dtao@genifast.edu.vn")
                .createdAt(Instant.now())
                .build();
        documentRepository.save(privateDoc);
        
        // Document 3: Project document
        Document projectDoc = Document.builder()
                .title("Kế hoạch chi tiết triển khai DMS GĐ2")
                .content("Dummy content")
                .type("vanban")
                .description("Kế hoạch chi tiết giai đoạn 2 của dự án DMS")
                .organization(organization)
                .department(departments.get("CNTT"))
                .category(null)
                .project(project)
                .confidentiality(DocumentConfidentiality.PROJECT.getValue())
                .status(1) // APPROVED
                .versionNumber(2)
                .createdBy("truongkhoa.cntt@genifast.edu.vn")
                .createdAt(Instant.now())
                .build();
        documentRepository.save(projectDoc);
        
        // Document 4: Public document
        Document publicDoc = Document.builder()
                .title("Kế hoạch đào tạo 2025")
                .content("Dummy content")
                .type("vanban")
                .description("Kế hoạch đào tạo năm học 2025-2026")
                .organization(organization)
                .department(departments.get("DTAO"))
                .category(quyCheCat)
                .confidentiality(DocumentConfidentiality.PUBLIC.getValue())
                .status(1) // APPROVED
                .versionNumber(1)
                .createdBy("chuyenvien.dtao@genifast.edu.vn")
                .createdAt(Instant.now())
                .build();
        documentRepository.save(publicDoc);
        
        // Tạo devices test
        Device companyDevice = Device.builder()
                .id("device-004")
                .deviceName("Laptop Dell Inspiron 15")
                .deviceType(DeviceType.COMPANY_DEVICE)
                .user(userRepository.findByEmail("chuyenvien.dtao@genifast.edu.vn").orElse(null))
                .status(1) // Active
                .registeredAt(Instant.now())
                .build();
        deviceRepository.save(companyDevice);
        
        Device externalDevice = Device.builder()
                .id("device-005")
                .deviceName("MacBook Pro M1")
                .deviceType(DeviceType.EXTERNAL_DEVICE)
                .user(userRepository.findByEmail("chuyenvien.dtao@genifast.edu.vn").orElse(null))
                .status(1) // Active
                .registeredAt(Instant.now())
                .build();
        deviceRepository.save(externalDevice);
        
        log.info(">>> Đã tạo 1 project, 2 categories, 4 documents và 2 devices test <<<");
    }

}
