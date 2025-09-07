package com.genifast.dms.config;

import com.genifast.dms.entity.Delegation;
import com.genifast.dms.entity.Document;
import com.genifast.dms.entity.DocumentVersion;
import com.genifast.dms.entity.Project;
import com.genifast.dms.entity.User;
import com.genifast.dms.entity.enums.DocumentType;
import com.genifast.dms.repository.DelegationRepository;
import com.genifast.dms.repository.DocumentRepository;
import com.genifast.dms.repository.DocumentVersionRepository;
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
    private final DocumentVersionRepository documentVersionRepository;

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

        // --- BƯỚC 1: KIỂM TRA QUYỀN CÓ SẴN TỪ ROLE (RBAC) ---
        // for (final GrantedAuthority authority : authentication.getAuthorities()) {
        // if (authority.getAuthority().equals(permissionString)) {
        // return true;
        // }
        // }

        User currentUser = userRepository.findByEmail(authentication.getName()).orElse(null);
        if (currentUser == null) {
            return false;
        }

        // --- BƯỚC 2: XỬ LÝ LOGIC PHÂN QUYỀN THEO NGỮ CẢNH CỦA ĐỐI TƯỢNG ---

        final String permissionString = permission.toString();
        Long id = getLongId(targetId);

        if ("document".equalsIgnoreCase(targetType)) {
            return hasDocumentPermission(currentUser, id, permissionString, authentication);
        }

        if ("project".equalsIgnoreCase(targetType)) {
            return hasProjectManagementPermission(currentUser, id, permissionString);
        }

        return false;
    }

    private boolean hasDocumentPermission(User user, Long documentId, String permission,
            Authentication authentication) {
        Document document = documentRepository.findById(documentId).orElse(null);
        if (document == null)
            return false;

        // **BỔ SUNG: Xử lý quyền documents:version:read ngay từ đầu**
        if (permission.startsWith("documents:version:read:")) {
            try {
                // Tách lấy version number từ chuỗi permission
                Integer versionNumber = Integer.parseInt(permission.substring("documents:version:read:".length()));
                return hasSpecificVersionPermission(user, documentId, versionNumber);
            } catch (NumberFormatException e) {
                // Nếu chuỗi permission không hợp lệ, từ chối quyền
                return false;
            }
        }

        // Ưu tiên 1: Quyền trong Dự án
        if (document.getProject() != null) {
            boolean hasPermissionInProject = hasProjectPermission(user, document.getProject(), permission);
            if (hasPermissionInProject)
                return true;
        }

        // Ưu tiên 2: Quyền qua Ủy quyền (Delegation)
        Optional<Delegation> delegation = delegationRepository.findActiveDelegation(user.getId(), documentId,
                permission, Instant.now());
        if (delegation.isPresent()) {
            DELEGATOR_HOLDER.set(delegation.get().getDelegator());
            // Người được ủy quyền vẫn phải tuân thủ các quy tắc ABAC của quyền đó
        } else {
            // Nếu không có ủy quyền, kiểm tra quyền RBAC gốc
            boolean hasRBACPermission = authentication.getAuthorities().stream()
                    .anyMatch(auth -> auth.getAuthority().equals(permission));
            if (!hasRBACPermission) {
                return false; // Không có quyền RBAC và không có ủy quyền -> Từ chối
            }
        }

        // Ưu tiên 3: Kiểm tra các điều kiện ABAC cho các quyền lai
        switch (permission) {
            case "documents:approve":
            case "documents:reject":
                // ABAC: Trạng thái tài liệu phải là PENDING (giả sử PENDING = 2)
                return document.getStatus() == 2;

            case "documents:submit":
                // ABAC: Trạng thái tài liệu phải là DRAFT (giả sử DRAFT = 1)
                return document.getStatus() == 1;

            case "documents:distribute":
                // ABAC: Phải là Trưởng phòng VÀ tài liệu đã được duyệt
                boolean isDeptManager = user.getIsDeptManager() != null && user.getIsDeptManager();
                return isDeptManager && document.getStatus() == 3; // APPROVED

            case "documents:publish":
                // ABAC: Phải là Manager (tổ chức hoặc phòng ban) VÀ tài liệu là loại THONG_BAO
                // VÀ đã duyệt
                boolean isManager = (user.getIsOrganizationManager() != null && user.getIsOrganizationManager())
                        || (user.getIsDeptManager() != null && user.getIsDeptManager());
                return isManager && document.getDocumentType() == DocumentType.NOTICE && document.getStatus() == 3;

            case "documents:track":
                // ABAC: User là người nhận HOẶC thuộc phòng ban của tài liệu
                boolean isRecipient = document.getRecipients().contains(user);
                // Đảm bảo user và document đều có phòng ban trước khi so sánh
                boolean isInDepartment = user.getDepartment() != null
                        && document.getDepartment() != null
                        && user.getDepartment().getId().equals(document.getDepartment().getId());
                return isRecipient || isInDepartment;

            case "documents:forward":
                // ABAC: User là người nhận và tài liệu đang ở trạng thái phù hợp (không phải
                // DRAFT hoặc REJECTED)
                boolean isRecipientForForward = document.getRecipients().contains(user);
                boolean isStatusOk = document.getStatus() != 1 && document.getStatus() != 4; // Not DRAFT or REJECTED
                return isRecipientForForward && isStatusOk;

            case "documents:export":
                // ABAC: Trạng thái phải là APPROVED VÀ user là manager của phòng ban đó
                boolean isDeptManagerForExport = user.getIsDeptManager() != null && user.getIsDeptManager()
                        && user.getDepartment() != null
                        && user.getDepartment().getId().equals(document.getDepartment().getId());
                return document.getStatus() == 3 && isDeptManagerForExport; // 3 = APPROVED

            case "documents:report":
                // Theo .docx: có thể lọc theo department và type
                // Logic ở đây là: user phải là manager và tài liệu phải thuộc loại có thể báo
                // cáo (ví dụ không phải loại KHAC)
                boolean isOrgManager = user.getIsOrganizationManager() != null && user.getIsOrganizationManager()
                        && user.getOrganization().getId().equals(document.getOrganization().getId());
                boolean isDeptManagerForReport = user.getIsDeptManager() != null && user.getIsDeptManager()
                        && user.getDepartment().getId().equals(document.getDepartment().getId());
                // Giả sử tất cả các loại tài liệu đều có thể báo cáo, việc lọc chi tiết sẽ do
                // business logic ở service xử lý
                return isOrgManager || isDeptManagerForReport;

            case "documents:notify":
                // ABAC: User phải là manager VÀ tài liệu là loại có thể thông báo rộng rãi
                boolean isManagerForNotify = (user.getIsOrganizationManager() != null
                        && user.getIsOrganizationManager())
                        || (user.getIsDeptManager() != null && user.getIsDeptManager());
                boolean isNotifiableType = (document.getDocumentType() == DocumentType.OUTGOING
                        || document.getDocumentType() == DocumentType.NOTICE);
                return isManagerForNotify && isNotifiableType;

            // Các quyền khác không có điều kiện ABAC phức tạp, nếu đã vượt qua
            // RBAC/Delegation thì được phép.
            default:
                return true;
        }

        // Ưu tiên 4: Quyền truy cập cơ bản (cho các quyền không có logic lai)
        // if (permission.equals("documents:read")) {
        // switch (document.getAccessType()) {
        // case 1:
        // return true; // Public
        // case 2:
        // return user.getOrganization() != null
        // && user.getOrganization().getId().equals(document.getOrganization().getId());
        // // Organization
        // case 3:
        // return user.getDepartment() != null
        // && user.getDepartment().getId().equals(document.getDepartment().getId()); //
        // Department
        // case 4:
        // return document.getCreatedBy().equals(user.getEmail()); // Private (chỉ người
        // tạo)
        // }
        // }

        // Mặc định: Nếu đã vượt qua kiểm tra RBAC/Delegation và không có điều kiện ABAC
        // đặc biệt nào, thì cho phép.
        // return true;
    }

    /**
     * BỔ SUNG: Phương thức mới chuyên để kiểm tra quyền trên một phiên bản cụ thể.
     * Đây là nơi logic ABAC cho version được thực thi.
     */
    private boolean hasSpecificVersionPermission(User user, Long documentId, Integer versionNumber) {
        // 1. Lấy thông tin phiên bản từ CSDL
        Optional<DocumentVersion> versionOpt = documentVersionRepository.findByDocumentIdAndVersionNumber(documentId,
                versionNumber);
        if (versionOpt.isEmpty()) {
            return false; // Phiên bản không tồn tại
        }
        DocumentVersion version = versionOpt.get();
        Document document = version.getDocument();

        // 2. Logic kiểm tra quyền truy cập cơ bản vào tài liệu gốc (quan trọng!)
        // User phải có quyền truy cập cơ bản vào tài liệu thì mới được xét đến quyền
        // xem phiên bản.
        boolean canAccessDocument = switch (document.getAccessType()) {
            case 1 -> true; // Public
            case 2 -> user.getOrganization() != null
                    && user.getOrganization().getId().equals(document.getOrganization().getId());
            case 3 ->
                user.getDepartment() != null && user.getDepartment().getId().equals(document.getDepartment().getId());
            case 4 -> document.getCreatedBy().equals(user.getEmail()) || document.getRecipients().contains(user);
            default -> false;
        };

        if (!canAccessDocument) {
            return false;
        }

        // 3. Logic ABAC dựa trên thuộc tính của phiên bản
        // Ví dụ: Một "Chuyên viên" chỉ được xem các phiên bản có status là DRAFT hoặc
        // PENDING
        // còn "Trưởng phòng" thì được xem cả phiên bản APPROVED.
        boolean isManager = (user.getIsOrganizationManager() != null && user.getIsOrganizationManager())
                || (user.getIsDeptManager() != null && user.getIsDeptManager());

        if (isManager) {
            return true; // Manager được xem tất cả các phiên bản của tài liệu họ có quyền truy cập.
        } else {
            // Nhân viên thông thường chỉ được xem các phiên bản chưa được duyệt (APPROVED)
            // Giả sử: 1 = DRAFT, 2 = PENDING, 3 = APPROVED
            return version.getStatus() < 3;
        }
    }

    private boolean hasProjectPermission(User user, Project project, String permission) {
        Instant now = Instant.now();
        if (project.getStatus() != 1 || now.isBefore(project.getStartDate()) || now.isAfter(project.getEndDate())) {
            return false; // ABAC: Dự án không hoạt động
        }

        return project.getMembers().stream()
                .filter(member -> member.getUser().getId().equals(user.getId()))
                .findFirst() // ABAC: User phải là thành viên
                .map(member -> member.getProjectRole().getPermissions().stream()
                        .anyMatch(p -> p.getName().equals(permission))) // RBAC trong dự án
                .orElse(false);
    }

    private boolean hasProjectManagementPermission(User user, Long projectId, String permission) {
        Project project = documentRepository.findById(projectId).get().getProject();
        if (project == null)
            return false;

        return hasProjectPermission(user, project, permission);
    }
}