package com.genifast.dms.security.abac;

import com.genifast.dms.entity.Document;
import com.genifast.dms.entity.User;
import com.genifast.dms.entity.enums.DeviceType;
import com.genifast.dms.repository.DocumentRepository;
import com.genifast.dms.repository.UserRepository;
import com.genifast.dms.service.abac.ABACService;
import com.genifast.dms.service.abac.AttributeExtractor;
import com.genifast.dms.service.abac.model.Environment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.time.Instant;
import java.util.Optional;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class RequirePermissionAspect {

    private final UserRepository userRepository;
    private final DocumentRepository documentRepository;
    private final ABACService abacService;
    private final AttributeExtractor attributeExtractor; // chỉ dùng để log minh bạch
    private final HttpServletRequest request;

    @Around("@annotation(com.genifast.dms.security.abac.RequirePermission)")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        Method method = ((MethodSignature) pjp.getSignature()).getMethod();
        RequirePermission rp = method.getAnnotation(RequirePermission.class);

        // 1) Lấy docId từ tham số method
        String docIdStr = extractDocId(method, pjp.getArgs());
        if (docIdStr == null) {
            // Không có docId -> để nguyên cho method xử lý (không áp ABAC theo tài liệu cụ thể)
            return pjp.proceed();
        }

        Long docId;
        try {
            docId = Long.valueOf(docIdStr);
        } catch (NumberFormatException ex) {
            throw new AccessDeniedException("Invalid docId format");
        }

        // 2) Lấy user hiện tại theo email đăng nhập
        String email = SecurityContextHolder.getContext().getAuthentication() != null
                ? SecurityContextHolder.getContext().getAuthentication().getName()
                : null;
        if (email == null) {
            throw new AccessDeniedException("Unauthenticated");
        }
        User user = userRepository.findByEmail(email).orElseThrow(() -> new AccessDeniedException("User not found"));

        // 3) Lấy document
        Document document = documentRepository.findById(docId)
                .orElseThrow(() -> new AccessDeniedException("Document not found"));

        // 4) Dựng Environment từ request
        Environment env = Environment.builder()
                .currentTime(Instant.now())
                .ipAddress(getClientIp(request))
                .deviceType(parseDeviceType(request.getHeader("Device-Type")))
                .deviceId(request.getHeader("Device-Id"))
                .sessionId(getSessionIdSafe())
                .userAgent(request.getHeader("User-Agent"))
                .build();

        // 5) Gọi engine ABAC hiện có để giữ nguyên kết quả phân quyền
        boolean allowed = abacService.hasPermission(user, document, rp.value(), env);
        if (!allowed) {
            throw new AccessDeniedException("ABAC denied by engine");
        }

        // 6) Log minh bạch các abacConditions (không thay đổi logic)
        try {
            var subject = attributeExtractor.extractSubjectAttributes(user);
            var resource = attributeExtractor.extractResourceAttributes(document);
            for (String cond : rp.abacConditions()) {
                switch (cond) {
                    case "department" -> {
                        boolean deptMatch = subject.getDepartmentId() != null && subject.getDepartmentId().equals(resource.getDepartmentId());
                        log.debug("ABACCondition[department]: userDept={} , docDept={} , match={}", subject.getDepartmentId(), resource.getDepartmentId(), deptMatch);
                    }
                    case "recipients" -> {
                        boolean inRecipients = resource.getRecipients() != null && resource.getRecipients().contains(subject.getUserId());
                        log.debug("ABACCondition[recipients]: userId={} inRecipients={}", subject.getUserId(), inRecipients);
                    }
                    default -> log.debug("ABACCondition[{}]: no-op (not explicitly evaluated here)", cond);
                }
            }
        } catch (Exception e) {
            log.debug("ABACCondition logging skipped: {}", e.getMessage());
        }

        // 7) Cho phép tiếp tục vào business method
        return pjp.proceed();
    }

    private String extractDocId(Method method, Object[] args) {
        Parameter[] params = method.getParameters();
        for (int i = 0; i < params.length; i++) {
            // Ưu tiên tham số có @PathVariable("docId")
            for (Annotation ann : params[i].getAnnotations()) {
                if (ann instanceof PathVariable pv) {
                    String name = ((PathVariable) ann).value();
                    if ("docId".equals(name) || name.isBlank()) {
                        Object v = args[i];
                        return v != null ? v.toString() : null;
                    }
                }
            }
            // Nếu không có annotation, thử tên tham số "docId"
            if ("docId".equals(params[i].getName())) {
                Object v = args[i];
                return v != null ? v.toString() : null;
            }
        }
        return null;
    }

    private DeviceType parseDeviceType(String header) {
        if (header == null || header.isBlank()) return DeviceType.COMPANY_DEVICE; // mặc định an toàn
        try {
            return DeviceType.valueOf(header.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return DeviceType.COMPANY_DEVICE;
        }
    }

    private String getClientIp(HttpServletRequest req) {
        if (req == null) return null;
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return req.getRemoteAddr();
    }

    private String getSessionIdSafe() {
        try {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            return auth != null && auth.getCredentials() != null ? String.valueOf(auth.getCredentials()) : null;
        } catch (Exception e) {
            return null;
        }
    }
}
