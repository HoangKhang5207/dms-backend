package com.genifast.dms.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.genifast.dms.common.constant.ErrorCode;
import com.genifast.dms.common.constant.ErrorMessage;
import com.genifast.dms.common.exception.ApiException;
import com.genifast.dms.dto.response.AuditLogResponse;
import com.genifast.dms.entity.AuditLog;
import com.genifast.dms.entity.Document;
import com.genifast.dms.entity.User;
import com.genifast.dms.mapper.AuditLogMapper;
import com.genifast.dms.repository.AuditLogRepository;
import com.genifast.dms.repository.DocumentRepository;
import com.genifast.dms.service.AuditLogService;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final DocumentRepository documentRepository;
    private final AuditLogMapper auditLogMapper;
    private final StringRedisTemplate redisTemplate; // Dùng StringRedisTemplate cho JSON
    private final ObjectMapper objectMapper; // Spring Boot tự động cung cấp bean này

    private static final String AUDIT_LOG_QUEUE_KEY = "dms:audit_log_queue";
    private ListOperations<String, String> listOps;

    @PostConstruct
    private void init() {
        listOps = redisTemplate.opsForList();
    }

    @Override
    @Async // Thực thi bất đồng bộ để không làm chậm request chính
    public void logAction(String action, String details, Long documentId, User user) {
        try {
            Document document = null;
            if (documentId != null) {
                document = documentRepository.findById(documentId).orElseThrow(
                        () -> new ApiException(ErrorCode.DOCUMENT_NOT_FOUND,
                                ErrorMessage.DOCUMENT_NOT_FOUND.getMessage()));
            }

            AuditLog auditLog = AuditLog.builder()
                    .user(user)
                    .document(document)
                    .action(action)
                    .details(details)
                    .ipAddress(getClientIp())
                    .sessionId(getSessionId())
                    .build();

            // Serialize đối tượng AuditLog thành chuỗi JSON và đẩy vào Redis list
            String logAsJson = objectMapper.writeValueAsString(auditLog);
            listOps.leftPush(AUDIT_LOG_QUEUE_KEY, logAsJson);
        } catch (Exception e) {
            log.error("Không thể đẩy log vào Redis: {}", e.getMessage(), e);
        }
    }

    @Scheduled(fixedRate = 15000) // Tăng thời gian lên 15 giây
    @Transactional
    public void flushAuditLogs() {
        // Lấy một lô log từ Redis (ví dụ: 100 log mỗi lần)
        List<String> logsAsJson = listOps.range(AUDIT_LOG_QUEUE_KEY, -100, -1);
        if (logsAsJson == null || logsAsJson.isEmpty()) {
            return;
        }

        List<AuditLog> logsToSave = logsAsJson.stream().map(json -> {
            try {
                // Deserialize JSON trở lại đối tượng AuditLog
                return objectMapper.readValue(json, AuditLog.class);
            } catch (Exception e) {
                log.error("Lỗi deserialize AuditLog từ Redis: {}", json, e);
                return null;
            }
        }).filter(java.util.Objects::nonNull).collect(Collectors.toList());

        if (!logsToSave.isEmpty()) {
            auditLogRepository.saveAll(logsToSave);
            // Cắt bỏ các log đã xử lý khỏi Redis list
            redisTemplate.opsForList().trim(AUDIT_LOG_QUEUE_KEY, 0, -101);
            log.info("Đã đẩy {} audit logs từ Redis vào CSDL.", logsToSave.size());
        }
    }

    @Override
    public Page<AuditLogResponse> getLogs(Pageable pageable) {
        return auditLogRepository.findAll(pageable).map(auditLogMapper::toAuditLogResponse);
    }

    @Override
    public Page<AuditLogResponse> getLogsByDocument(Long documentId, Pageable pageable) {
        return auditLogRepository.findByDocumentId(documentId, pageable).map(auditLogMapper::toAuditLogResponse);
    }

    // --- Helper Methods ---
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
        return (request != null) ? request.getSession().getId() : "N/A";
    }

    private HttpServletRequest getCurrentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes) {
            return ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        }
        return null;
    }
}