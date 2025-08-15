package com.genifast.dms.service.abac;

import com.genifast.dms.entity.*;
import com.genifast.dms.entity.enums.*;
import com.genifast.dms.service.abac.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * ABAC Service - Attribute-Based Access Control
 * Triển khai phân quyền dựa trên thuộc tính với cache Redis
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ABACService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final AttributeExtractor attributeExtractor;
    private final PolicyEvaluator policyEvaluator;
    
    private static final String PERMISSION_CACHE_PREFIX = "abac:permission:";
    private static final String POLICY_CACHE_PREFIX = "abac:policy:";
    private static final int CACHE_TTL_SECONDS = 300; // 5 phút

    /**
     * Kiểm tra quyền truy cập chính
     */
    public boolean hasPermission(User user, Document document, String action, Environment environment) {
        try {
            // 1. Tạo cache key
            String cacheKey = generateCacheKey(user.getId(), document.getId(), action, environment);
            
            // 2. Kiểm tra cache trước
            Boolean cachedResult = getCachedPermission(cacheKey);
            if (cachedResult != null) {
                log.debug("Cache hit for permission check: user={}, doc={}, action={}, result={}", 
                    user.getId(), document.getId(), action, cachedResult);
                return cachedResult;
            }

            // 3. Trích xuất thuộc tính
            SubjectAttributes subjectAttrs = attributeExtractor.extractSubjectAttributes(user);
            ResourceAttributes resourceAttrs = attributeExtractor.extractResourceAttributes(document);
            ActionAttributes actionAttrs = new ActionAttributes(action);

            // 4. Đánh giá policies
            boolean result = evaluatePermission(subjectAttrs, resourceAttrs, actionAttrs, environment);

            // 5. Lưu vào cache
            cachePermission(cacheKey, result);

            log.info("Permission evaluated: user={}, doc={}, action={}, result={}", 
                user.getId(), document.getId(), action, result);
            
            return result;

        } catch (Exception e) {
            log.error("Error evaluating permission for user={}, doc={}, action={}", 
                user.getId(), document.getId(), action, e);
            return false; // Fail-safe: từ chối quyền khi có lỗi
        }
    }

    /**
     * Đánh giá quyền dựa trên các thuộc tính
     */
    private boolean evaluatePermission(SubjectAttributes subject, ResourceAttributes resource, 
                                     ActionAttributes action, Environment environment) {
        
        // 1. Kiểm tra quy tắc ABAC theo phòng ban
        if (!evaluateDepartmentRules(subject, resource, action)) {
            return false;
        }

        // 2. Kiểm tra quy tắc ABAC theo thiết bị
        if (!evaluateDeviceRules(subject, resource, environment)) {
            return false;
        }

        // 3. Kiểm tra quy tắc ABAC theo dự án (nếu có)
        if (resource.getProjectId() != null) {
            if (!evaluateProjectRules(subject, resource, action, environment)) {
                return false;
            }
        }

        // 4. Kiểm tra quy tắc bảo mật tài liệu
        if (!evaluateConfidentialityRules(subject, resource, action)) {
            return false;
        }

        // 5. Kiểm tra quy tắc thời gian (nếu có)
        if (!evaluateTimeRules(subject, resource, environment)) {
            return false;
        }

        return true;
    }

    /**
     * Quy tắc ABAC theo phòng ban
     */
    private boolean evaluateDepartmentRules(SubjectAttributes subject, ResourceAttributes resource, ActionAttributes action) {
        // Nguyên tắc nội bộ: Phòng ban có toàn quyền với tài liệu của mình
        if (subject.getDepartmentId() != null && subject.getDepartmentId().equals(resource.getDepartmentId())) {
            return true;
        }

        // Nguyên tắc cấp bậc quản lý: BGH có quyền xem tất cả (trừ PRIVATE không được cấp phép)
        if ("BGH".equals(subject.getDepartmentCode()) && 
            !DocumentConfidentiality.PRIVATE.equals(resource.getConfidentiality())) {
            return true;
        }

        // Nguyên tắc liên quan nghiệp vụ: Kiểm tra quyền chia sẻ
        return policyEvaluator.hasSharedPermission(subject.getUserId(), resource.getDocumentId(), action.getAction());
    }

    /**
     * Quy tắc ABAC theo thiết bị
     */
    private boolean evaluateDeviceRules(SubjectAttributes subject, ResourceAttributes resource, Environment environment) {
        DeviceType deviceType = environment.getDeviceType();
        
        // Tài liệu PRIVATE hoặc LOCKED chỉ được truy cập từ thiết bị công ty
        if ((DocumentConfidentiality.PRIVATE.equals(resource.getConfidentiality()) ||
             DocumentConfidentiality.LOCKED.equals(resource.getConfidentiality())) &&
            !DeviceType.COMPANY_DEVICE.equals(deviceType)) {
            return false;
        }

        // Tài liệu PUBLIC có thể truy cập từ thiết bị ngoài (với hạn chế)
        if (DocumentConfidentiality.PUBLIC.equals(resource.getConfidentiality()) &&
            DeviceType.EXTERNAL_DEVICE.equals(deviceType)) {
            // Visitor chỉ xem được 1/3 số trang
            return subject.getRoles().contains("VISITOR") ? 
                evaluateVisitorAccess(resource) : true;
        }

        return DeviceType.COMPANY_DEVICE.equals(deviceType);
    }

    /**
     * Quy tắc ABAC theo dự án
     */
    private boolean evaluateProjectRules(SubjectAttributes subject, ResourceAttributes resource, 
                                       ActionAttributes action, Environment environment) {
        Long projectId = resource.getProjectId();
        
        // Kiểm tra tư cách thành viên dự án
        if (!policyEvaluator.isProjectMember(subject.getUserId(), projectId)) {
            return false;
        }

        // Kiểm tra thời gian hiệu lực dự án
        return policyEvaluator.isProjectActive(projectId, environment.getCurrentTime());
    }

    /**
     * Quy tắc bảo mật tài liệu
     */
    private boolean evaluateConfidentialityRules(SubjectAttributes subject, ResourceAttributes resource, ActionAttributes action) {
        DocumentConfidentiality confidentiality = resource.getConfidentiality();
        
        switch (confidentiality) {
            case PUBLIC:
                return true;
            case INTERNAL:
                return subject.getOrganizationId().equals(resource.getOrganizationId());
            case PRIVATE:
                return policyEvaluator.hasPrivateDocAccess(subject.getUserId(), resource.getDocumentId());
            case LOCKED:
                return subject.getRoles().contains("HIEU_TRUONG") || 
                       subject.getRoles().contains("QUAN_TRI_VIEN");
            case PROJECT:
                return resource.getProjectId() != null && 
                       policyEvaluator.isProjectMember(subject.getUserId(), resource.getProjectId());
            case EXTERNAL:
                // Cho phép theo cơ chế chia sẻ tài liệu: user phải có DocumentPermission phù hợp với action
                return policyEvaluator.hasSharedPermission(subject.getUserId(), resource.getDocumentId(), action.getAction());
            default:
                return false;
        }
    }

    /**
     * Quy tắc thời gian
     */
    private boolean evaluateTimeRules(SubjectAttributes subject, ResourceAttributes resource, Environment environment) {
        // Kiểm tra thời hạn chia sẻ tài liệu (nếu có)
        return policyEvaluator.isWithinShareTimebound(subject.getUserId(), resource.getDocumentId(), 
                                                     environment.getCurrentTime());
    }

    /**
     * Đánh giá quyền truy cập của Visitor
     */
    private boolean evaluateVisitorAccess(ResourceAttributes resource) {
        // Visitor chỉ xem được 1/3 số trang tài liệu PUBLIC
        return DocumentConfidentiality.PUBLIC.equals(resource.getConfidentiality());
    }

    /**
     * Tạo cache key
     */
    private String generateCacheKey(Long userId, Long docId, String action, Environment environment) {
        return String.format("%s%d:%d:%s:%s:%d", 
            PERMISSION_CACHE_PREFIX, userId, docId, action, 
            environment.getDeviceType().name(), environment.getCurrentTime().getEpochSecond() / 3600); // Group by hour
    }

    /**
     * Lấy kết quả từ cache
     */
    private Boolean getCachedPermission(String cacheKey) {
        try {
            return (Boolean) redisTemplate.opsForValue().get(cacheKey);
        } catch (Exception e) {
            log.warn("Error getting cached permission: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Lưu kết quả vào cache
     */
    private void cachePermission(String cacheKey, boolean result) {
        try {
            redisTemplate.opsForValue().set(cacheKey, result, Duration.ofSeconds(CACHE_TTL_SECONDS));
        } catch (Exception e) {
            log.warn("Error caching permission: {}", e.getMessage());
        }
    }

    /**
     * Xóa cache khi có thay đổi quyền
     */
    public void invalidateUserPermissions(Long userId) {
        try {
            String pattern = PERMISSION_CACHE_PREFIX + userId + ":*";
            redisTemplate.delete(redisTemplate.keys(pattern));
            log.info("Invalidated permission cache for user: {}", userId);
        } catch (Exception e) {
            log.warn("Error invalidating user permissions cache: {}", e.getMessage());
        }
    }

    /**
     * Xóa cache khi có thay đổi tài liệu
     */
    public void invalidateDocumentPermissions(Long docId) {
        try {
            String pattern = PERMISSION_CACHE_PREFIX + "*:" + docId + ":*";
            redisTemplate.delete(redisTemplate.keys(pattern));
            log.info("Invalidated permission cache for document: {}", docId);
        } catch (Exception e) {
            log.warn("Error invalidating document permissions cache: {}", e.getMessage());
        }
    }
}
