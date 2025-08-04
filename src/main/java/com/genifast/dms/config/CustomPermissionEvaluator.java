package com.genifast.dms.config;

import com.genifast.dms.entity.User;
import com.genifast.dms.repository.DelegationRepository;
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
        return delegationRepository.findActiveDelegation(
                userId,
                documentId,
                permission.toString(),
                Instant.now()).isPresent();
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
        // Phương thức này ít được sử dụng hơn, nhưng chúng ta vẫn implement để đầy đủ.
        // Spring sẽ gọi phương thức trên (2 tham số) nếu targetDomainObject được truyền
        // trực tiếp.
        // Nó sẽ gọi phương thức này (3 tham số) nếu chúng ta truyền ID và Type.
        return hasPermission(authentication, targetId, permission);
    }
}