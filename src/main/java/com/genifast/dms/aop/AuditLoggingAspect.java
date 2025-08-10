package com.genifast.dms.aop;

import com.genifast.dms.common.utils.JwtUtils;
import com.genifast.dms.config.CustomPermissionEvaluator;
import com.genifast.dms.dto.request.AuditLogRequest;
import com.genifast.dms.dto.request.DelegationRequest;
import com.genifast.dms.dto.request.DocumentCommentRequest;
import com.genifast.dms.dto.request.DocumentShareRequest;
import com.genifast.dms.dto.request.DocumentUpdateRequest;
import com.genifast.dms.dto.request.LoginRequestDto;
import com.genifast.dms.dto.request.SignUpRequestDto;
import com.genifast.dms.dto.response.DocumentResponse;
import com.genifast.dms.entity.User;
import com.genifast.dms.repository.UserRepository;
import com.genifast.dms.service.AuditLogService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Optional;
import java.util.StringJoiner;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditLoggingAspect {

    private final AuditLogService auditLogService;
    private final UserRepository userRepository;

    @AfterReturning(pointcut = "@annotation(auditLog)", returning = "result")
    public void logAfterReturning(JoinPoint joinPoint, AuditLog auditLog, Object result) {
        try {
            Optional<String> currentUserEmailOpt = JwtUtils.getCurrentUserLogin();
            if (currentUserEmailOpt.isEmpty()) {
                log.warn("Cannot log audit action because user is not authenticated.");
                return;
            }

            Optional<User> userOpt = userRepository.findByEmail(currentUserEmailOpt.get());
            if (userOpt.isEmpty()) {
                log.warn("Cannot log audit action because user {} not found in database.", currentUserEmailOpt.get());
                return;
            }

            String action = auditLog.action();
            Long documentId = findDocumentId(joinPoint);

            // Lấy thông tin người ủy quyền từ ThreadLocal
            User delegatedBy = CustomPermissionEvaluator.DELEGATOR_HOLDER.get();
            Long delegatedByUserId = (delegatedBy != null) ? delegatedBy.getId() : null;

            String details = createDetailedMessage(joinPoint, action, result, delegatedBy);

            AuditLogRequest logRequest = AuditLogRequest.builder()
                    .action(action)
                    .details(details)
                    .documentId(documentId)
                    .userId(userOpt.get().getId()) // Chỉ lưu ID của user
                    .delegatedByUserId(delegatedByUserId) // Chỉ lưu ID của người ủy quyền
                    .ipAddress(getClientIp())
                    .sessionId(getSessionId())
                    .build();

            auditLogService.logAction(logRequest);

            CustomPermissionEvaluator.DELEGATOR_HOLDER.remove(); // Dọn dẹp sau khi dùng xong
        } catch (Exception e) {
            log.error("Error while auditing action: {}", e.getMessage(), e);
        }
    }

    private Long findDocumentId(JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Parameter[] parameters = signature.getMethod().getParameters();

        for (int i = 0; i < parameters.length; i++) {
            // Tìm tham số có tên chứa "doc" hoặc "id" và có kiểu Long
            if ((parameters[i].getName().toLowerCase().contains("doc")
                    || parameters[i].getName().equalsIgnoreCase("id"))
                    && args[i] instanceof Long) {
                return (Long) args[i];
            }
        }
        // Nếu không tìm thấy, thử tìm trong đối tượng request
        for (Object arg : args) {
            try {
                Method getDocumentId = arg.getClass().getMethod("getDocumentId");
                Object docIdObj = getDocumentId.invoke(arg);
                if (docIdObj instanceof Long) {
                    return (Long) docIdObj;
                }
            } catch (Exception e) {
                // Ignore
            }
        }
        return null;
    }

    private String createDetailedMessage(JoinPoint joinPoint, String action, Object result, User delegatedBy) {
        Object[] args = joinPoint.getArgs();

        String delegationInfo = "";
        if (delegatedBy != null) {
            delegationInfo = String.format(" (thông qua ủy quyền từ %s)", delegatedBy.getEmail());
        }

        switch (action) {
            case "CREATE_UPLOAD_DOCUMENT":
                if (result instanceof DocumentResponse) {
                    DocumentResponse res = (DocumentResponse) result;
                    return String.format("Tải lên và tạo mới tài liệu '%s' (ID: %d). %s", res.getTitle(), res.getId(),
                            delegationInfo);
                }
                return "Tạo mới một tài liệu.";
            case "READ_DOCUMENT":
                if (args.length > 0 && args[0] instanceof Long) {
                    return String.format("Lấy thông tin tài liệu ID %s.%s", args[0], delegationInfo);
                }
                return "Lấy thông tin tài liệu.";
            case "DOWNLOAD_DOCUMENT":
                if (args.length > 0 && args[0] instanceof Long) {
                    return String.format("Tải xuống tài liệu ID %s.%s", args[0], delegationInfo);
                }
                return "Tải xuống tài liệu.";
            case "UPDATE_DOCUMENT":
                if (args.length > 1 && args[1] instanceof DocumentUpdateRequest) {
                    return String.format("Cập nhật thông tin tài liệu ID %s.%s", args[0], delegationInfo);
                }
                return String.format("Cập nhật thông tin tài liệu ID %s.%s", args[0], delegationInfo);
            case "DELETE_DOCUMENT":
                return String.format("Xóa tài liệu ID %s.%s", args[0], delegationInfo);
            case "APPROVE_DOCUMENT":
                return String.format("Phê duyệt tài liệu ID %s.%s", args[0], delegationInfo);
            case "REJECT_DOCUMENT":
                return String.format("Từ chối tài liệu ID %s.%s", args[0], delegationInfo);
            case "SHARE_DOCUMENT":
                if (args.length > 1 && args[1] instanceof DocumentShareRequest) {
                    DocumentShareRequest req = (DocumentShareRequest) args[1];
                    // Logic để mô tả chi tiết các loại share
                    String shareDetails = "";
                    if (req.getExpiryDate() != null)
                        shareDetails += " (có thời hạn)";
                    if (req.getIsShareToExternal() != null && req.getIsShareToExternal())
                        shareDetails += " (ra ngoài tổ chức)";
                    return String.format("Chia sẻ tài liệu ID %s cho '%s'%s.%s", args[0], req.getRecipientEmail(),
                            shareDetails, delegationInfo);
                }
                return String.format("Chia sẻ công khai cho tài liệu ID %s.%s", args[0], delegationInfo);
            case "TRACK_DOCUMENT":
                return String.format("Theo dõi lịch sử của tài liệu ID %s.%s", args[0], delegationInfo);
            case "ARCHIVE_DOCUMENT":
                return String.format("Lưu trữ tài liệu ID %s.%s", args[0], delegationInfo);
            case "SUBMIT_DOCUMENT":
                return String.format("Trình ký tài liệu ID %s.%s", args[0], delegationInfo);
            case "PUBLISH_DOCUMENT":
                return String.format("Công khai tài liệu ID %s.%s", args[0], delegationInfo);
            case "SIGN_DOCUMENT":
                return String.format("Ký điện tử tài liệu ID %s.%s", args[0], delegationInfo);
            case "LOCK_DOCUMENT":
                return String.format("Khóa tài liệu ID %s để ngăn chỉnh sửa.%s", args[0], delegationInfo);
            case "UNLOCK_DOCUMENT":
                return String.format("Mở khóa tài liệu ID %s.%s", args[0], delegationInfo);
            case "ADD_COMMENT":
                if (args.length > 1 && args[1] instanceof DocumentCommentRequest) {
                    return String.format("Thêm bình luận vào tài liệu ID %s.%s", args[0], delegationInfo);
                }
                return String.format("Thêm bình luận vào tài liệu ID %s.%s", args[0], delegationInfo);
            case "RESTORE_DOCUMENT":
                return String.format("Khôi phục tài liệu ID %s từ lưu trữ.%s", args[0], delegationInfo);
            case "VIEW_HISTORY":
                return String.format("Xem lịch sử phiên bản của tài liệu ID %s.%s", args[0], delegationInfo);
            case "VIEW_SPECIFIC_VERSION":
                return String.format("Xem phiên bản %s của tài liệu ID %s.%s", args[1], args[0], delegationInfo);
            case "NOTIFY_RECIPIENTS":
                return String.format("Gửi thông báo liên quan đến tài liệu ID %s.%s", args[0], delegationInfo);
            case "EXPORT_DOCUMENT":
                return String.format("Xuất/tải về tài liệu ID %s.%s", args[0], delegationInfo);
            case "FORWARD_DOCUMENT":
                return String.format("Chuyển tiếp tài liệu ID %s cho người dùng '%s'.%s", args[0], args[1],
                        delegationInfo);
            case "DISTRIBUTE_DOCUMENT":
                return String.format("Phân phối tài liệu ID %s đến phòng ban ID %s.%s", args[0], args[1],
                        delegationInfo);
            case "GENERATE_DOCUMENT_REPORT":
                return String.format("Tạo và xuất báo cáo tài liệu loại '%s'.%s", args[0], delegationInfo);
            case "DELEGATE_PERMISSION":
                if (args[0] instanceof DelegationRequest) {
                    DelegationRequest req = (DelegationRequest) args[0];
                    return String.format("Ủy quyền quyền '%s' trên tài liệu ID %d cho người dùng ID %d.",
                            req.getPermission(), req.getDocumentId(), req.getDelegateeId());
                }
                break;
            case "REVOKE_DELEGATION":
                return String.format("Thu hồi ủy quyền ID %s.", args[0]);

            // --- SECURITY LOGS ---
            case "USER_LOGIN":
                if (args[0] instanceof LoginRequestDto) {
                    return String.format("Người dùng '%s' đăng nhập thành công.",
                            ((LoginRequestDto) args[0]).getEmail());
                }
                return "Người dùng đăng nhập thành công.";
            case "USER_SIGNUP":
                if (args[0] instanceof SignUpRequestDto) {
                    return String.format("Người dùng mới '%s' đã đăng ký.", ((SignUpRequestDto) args[0]).getEmail());
                }
                return "Người dùng mới đã đăng ký.";
        }

        // Fallback về message chung nếu không có case nào khớp
        StringJoiner joiner = new StringJoiner(", ");
        for (Object arg : args) {
            if (arg != null)
                joiner.add(arg.toString());
        }
        return String.format("Thực thi phương thức %s với tham số: [%s].", joinPoint.getSignature().getName(),
                joiner.toString());
    }

    // --- CÁC PHƯƠNG THỨC HELPER MỚI ---
    private String getClientIp() {
        HttpServletRequest request = getCurrentRequest();
        if (request == null)
            return "N/A";

        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isEmpty()) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }

    private String getSessionId() {
        HttpServletRequest request = getCurrentRequest();
        return (request != null && request.getSession(false) != null) ? request.getSession().getId() : "N/A";
    }

    private HttpServletRequest getCurrentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes) {
            return ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        }
        return null;
    }
}