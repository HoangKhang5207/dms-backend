package com.genifast.dms.config;

import com.genifast.dms.entity.Delegation;
import com.genifast.dms.entity.Document;
import com.genifast.dms.entity.Project;
import com.genifast.dms.entity.User;
import com.genifast.dms.repository.DelegationRepository;
import com.genifast.dms.repository.DocumentRepository;
import com.genifast.dms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.time.Instant;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CustomPermissionEvaluator implements PermissionEvaluator {

    private final DelegationRepository delegationRepository;
    private final UserRepository userRepository;
    private final DocumentRepository documentRepository;

    // Sử dụng ThreadLocal để lưu thông tin người ủy quyền trong phạm vi một request
    public static final ThreadLocal<User> DELEGATOR_HOLDER = new ThreadLocal<>();

    /**
     * Phương thức này được Spring Security gọi khi
     * dùng @PreAuthorize("hasPermission(...)")
     *
     * @param authentication     Đối tượng chứa thông tin người dùng đang đăng nhập.
     * @param targetDomainObject ID của đối tượng cần kiểm tra (vd: documentId).
     * @param permission         Quyền cần kiểm tra (vd: 'documents:approve').
     * @return true nếu có quyền, false nếu không.
     */
    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
        DELEGATOR_HOLDER.remove(); // Xóa thông tin cũ trước khi kiểm tra

        if ((authentication == null) || !(permission instanceof String)) {
            return false;
        }

        // --- BƯỚC 1: KIỂM TRA QUYỀN CÓ SẴN TỪ ROLE (RBAC) ---
        // Đây là cách kiểm tra quyền nhanh nhất. Nếu người dùng đã có quyền này thông
        // qua vai trò của họ, chúng ta không cần kiểm tra ủy quyền nữa.
        for (final GrantedAuthority authority : authentication.getAuthorities()) {
            if (authority.getAuthority().equals(permission.toString())) {
                return true;
            }
        }

        // --- BƯỚC 2: KIỂM TRA QUYỀN TỪ ỦY QUYỀN (DELEGATION - ABAC) ---
        // Chỉ thực hiện nếu bước 1 thất bại và có đầy đủ thông tin.
        if (targetDomainObject == null) {
            return false; // Không có đối tượng cụ thể (document) thì không thể kiểm tra ủy quyền.
        }

        // Lấy thông tin người dùng hiện tại từ CSDL
        Optional<User> currentUserOpt = userRepository.findByEmail(authentication.getName());
        if (currentUserOpt.isEmpty()) {
            return false; // Không tìm thấy người dùng
        }

        Long userId = currentUserOpt.get().getId();
        Long documentId = getLongId(targetDomainObject);

        // Truy vấn CSDL để tìm một ủy quyền còn hiệu lực
        Optional<Delegation> activeDelegationOpt = delegationRepository.findActiveDelegation(
                userId,
                documentId,
                permission.toString(),
                Instant.now());

        if (activeDelegationOpt.isPresent()) {
            // Nếu có ủy quyền, lưu lại người đã ủy quyền và trả về true
            DELEGATOR_HOLDER.set(activeDelegationOpt.get().getDelegator());
            return true;
        }

        return false;
    }

    /**
     * Xử lý các trường hợp targetId có kiểu dữ liệu khác nhau.
     */
    private Long getLongId(Object idObject) {
        if (idObject instanceof Long) {
            return (Long) idObject;
        }
        try {
            return Long.parseLong(idObject.toString());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Không thể chuyển đổi ID đối tượng thành Long: " + idObject, e);
        }
    }

    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType,
            Object permission) {
        DELEGATOR_HOLDER.remove();

        if ((authentication == null) || (targetType == null) || !(permission instanceof String)) {
            return false;
        }

        final String permissionString = permission.toString();

        // --- BƯỚC 1: KIỂM TRA QUYỀN CÓ SẴN TỪ ROLE (RBAC) ---
        for (final GrantedAuthority authority : authentication.getAuthorities()) {
            if (authority.getAuthority().equals(permissionString)) {
                return true;
            }
        }

        User currentUser = userRepository.findByEmail(authentication.getName()).orElse(null);
        if (currentUser == null) {
            return false;
        }

        // --- BƯỚC 2: XỬ LÝ LOGIC PHÂN QUYỀN THEO NGỮ CẢNH CỦA ĐỐI TƯỢNG ---

        Long id = getLongId(targetId);

        if ("document".equalsIgnoreCase(targetType)) {
            return hasDocumentPermission(currentUser, id, permissionString);
        }

        if ("project".equalsIgnoreCase(targetType)) {
            return hasProjectManagementPermission(currentUser, id, permissionString);
        }

        return false;
    }

    private boolean hasDocumentPermission(User user, Long documentId, String permission) {
        Document document = documentRepository.findById(documentId).orElse(null);
        if (document == null)
            return false;

        // ƯU TIÊN 1: KIỂM TRA QUYỀN TRONG DỰ ÁN (NẾU CÓ)
        if (document.getProject() != null) {
            Project project = document.getProject();

            // ABAC check: Dự án có đang hoạt động không?
            Instant now = Instant.now();
            if (project.getStatus() != 1 || now.isBefore(project.getStartDate()) || now.isAfter(project.getEndDate())) {
                return false; // Dự án không hoạt động
            }

            // RBAC check: Vai trò của user trong dự án có quyền này không?
            boolean hasPermissionInProject = project.getMembers().stream()
                    .filter(member -> member.getUser().getId().equals(user.getId()))
                    .findFirst()
                    .map(member -> member.getProjectRole().getPermissions().stream()
                            .anyMatch(p -> p.getName().equals(permission)))
                    .orElse(false);

            if (hasPermissionInProject) {
                return true; // Nếu có quyền trong dự án, cho phép ngay
            }
        }

        // ƯU TIÊN 2: KIỂM TRA QUYỀN ỦY QUYỀN (DELEGATION)
        Optional<Delegation> delegation = delegationRepository.findActiveDelegation(user.getId(), documentId,
                permission, Instant.now());
        if (delegation.isPresent()) {
            DELEGATOR_HOLDER.set(delegation.get().getDelegator());
            return true;
        }

        // ƯU TIÊN 3: KIỂM TRA QUYỀN TRUY CẬP CƠ BẢN (NẾU KHÔNG CÓ DỰ ÁN HOẶC KHÔNG CÓ
        // QUYỀN TRONG DỰ ÁN)
        // Logic này dành cho các quyền cơ bản như 'documents:read'
        if (permission.equals("documents:read")) {
            switch (document.getAccessType()) {
                case 1:
                    return true; // Public
                case 2:
                    return user.getOrganization() != null
                            && user.getOrganization().getId().equals(document.getOrganization().getId()); // Organization
                case 3:
                    return user.getDepartment() != null
                            && user.getDepartment().getId().equals(document.getDepartment().getId()); // Department
                case 4:
                    return document.getCreatedBy().equals(user.getEmail()); // Private (chỉ người tạo)
            }
        }

        return false; // Mặc định từ chối
    }

    private boolean hasProjectManagementPermission(User user, Long projectId, String permission) {
        // Tương tự logic đã có, kiểm tra user có phải là thành viên dự án
        // và vai trò của họ có quyền quản lý dự án không.
        // Đây là nơi để kiểm tra các quyền như 'project:manage',
        // 'project:member:manage'
        Project project = documentRepository.findById(projectId).get().getProject(); // Lấy project từ document
        if (project == null)
            return false;

        Instant now = Instant.now();
        if (now.isBefore(project.getStartDate()) || now.isAfter(project.getEndDate())) {
            return false;
        }

        return project.getMembers().stream()
                .filter(member -> member.getUser().getId().equals(user.getId()))
                .anyMatch(member -> member.getProjectRole().getPermissions().stream()
                        .anyMatch(p -> p.getName().equals(permission)));
    }
}