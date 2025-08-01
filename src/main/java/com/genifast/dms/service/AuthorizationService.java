package com.genifast.dms.service;

import org.springframework.stereotype.Service;
import com.genifast.dms.entity.User;
import com.genifast.dms.entity.Document;
import com.genifast.dms.entity.Department;
import com.genifast.dms.repository.UserRepository;
import com.genifast.dms.repository.DocumentRepository;
import com.genifast.dms.repository.DepartmentRepository;
import com.genifast.dms.common.utils.JwtUtils;
import com.genifast.dms.common.exception.ApiException;
import com.genifast.dms.common.constant.ErrorCode;
import com.genifast.dms.common.constant.ErrorMessage;
import lombok.RequiredArgsConstructor;

@Service("authService")
@RequiredArgsConstructor
public class AuthorizationService {

    private final UserRepository userRepository;
    private final DocumentRepository documentRepository;
    private final DepartmentRepository departmentRepository;

    private User getCurrentUser() {
        String email = JwtUtils.getCurrentUserLogin()
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND, "User not authenticated."));
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND, ErrorMessage.INVALID_USER.getMessage()));
    }

    /**
     * Kiểm tra người dùng hiện tại có phải là Manager của Tổ chức không.
     */
    public boolean isOrgManager(Long organizationId) {
        User user = getCurrentUser();
        if (user.getOrganization() == null || user.getIsOrganizationManager() == null) {
            return false;
        }
        return user.getIsOrganizationManager() && user.getOrganization().getId().equals(organizationId);
    }

    /**
     * Kiểm tra người dùng hiện tại có phải là Manager của Phòng ban không.
     */
    public boolean isDeptManager(Long departmentId) {
        User user = getCurrentUser();
        Department department = departmentRepository.findById(departmentId).orElse(null);
        if (user.getDepartment() == null || user.getIsDeptManager() == null || department == null) {
            return false;
        }
        return user.getIsDeptManager() && user.getDepartment().getId().equals(departmentId);
    }

    /**
     * Kiểm tra người dùng hiện tại có phải là thành viên của Tổ chức không.
     */
    public boolean isMemberOfOrg(Long organizationId) {
        User user = getCurrentUser();
        if (user.getOrganization() == null) {
            return false;
        }
        return user.getOrganization().getId().equals(organizationId);
    }

    /**
     * Kiểm tra người dùng có quyền truy cập vào một tài liệu cụ thể hay không.
     * Logic này được tái cấu trúc từ
     * DocumentServiceImpl.authorizeUserCanAccessDocument
     */
    public boolean canAccessDocument(Long docId) {
        User user = getCurrentUser();
        Document document = documentRepository.findById(docId)
                .orElseThrow(() -> new ApiException(ErrorCode.DOCUMENT_NOT_FOUND,
                        ErrorMessage.DOCUMENT_NOT_FOUND.getMessage()));

        switch (document.getAccessType()) {
            case 1: // Public
                return true;
            case 2: // Organization
                return user.getOrganization() != null
                        && user.getOrganization().getId().equals(document.getOrganization().getId());
            case 3: // Department
                return user.getDepartment() != null
                        && user.getDepartment().getId().equals(document.getDepartment().getId());
            case 4: // Private
                // Logic kiểm tra private access sẽ phức tạp hơn, tạm thời kiểm tra người tạo
                return document.getCreatedBy().equals(user.getEmail());
            default:
                return false;
        }
    }

    /**
     * Kiểm tra xem người dùng có quyền chỉnh sửa tài liệu hay không (Người tạo, Org
     * Manager, Dept Manager).
     */
    public boolean canEditDocument(Long docId) {
        User user = getCurrentUser();
        Document document = documentRepository.findById(docId)
                .orElseThrow(() -> new ApiException(ErrorCode.DOCUMENT_NOT_FOUND,
                        ErrorMessage.DOCUMENT_NOT_FOUND.getMessage()));

        // Người tạo có quyền chỉnh sửa
        if (document.getCreatedBy().equals(user.getEmail())) {
            return true;
        }

        // Manager của tổ chức chứa tài liệu có quyền
        if (user.getIsOrganizationManager() != null && user.getIsOrganizationManager() &&
                user.getOrganization() != null
                && user.getOrganization().getId().equals(document.getOrganization().getId())) {
            return true;
        }

        // Manager của phòng ban chứa tài liệu có quyền
        if (user.getIsDeptManager() != null && user.getIsDeptManager() &&
                user.getDepartment() != null && user.getDepartment().getId().equals(document.getDepartment().getId())) {
            return true;
        }

        return false;
    }
}