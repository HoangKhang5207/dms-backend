package com.genifast.dms.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.genifast.dms.dto.request.AuditLogRequest;
import com.genifast.dms.dto.response.AuditLogResponse;
import com.genifast.dms.entity.AuditLog;
import com.genifast.dms.entity.Document;
import com.genifast.dms.entity.User;
import com.genifast.dms.mapper.AuditLogMapper;
import com.genifast.dms.repository.AuditLogRepository;
import com.genifast.dms.repository.DocumentRepository;
import com.genifast.dms.repository.UserRepository;
import com.genifast.dms.service.AuditLogService;

import jakarta.annotation.PostConstruct;
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

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final AuditLogMapper auditLogMapper;
    private final StringRedisTemplate redisTemplate; // Dùng StringRedisTemplate cho JSON
    private final ObjectMapper objectMapper;

    private static final String AUDIT_LOG_QUEUE_KEY = "dms:audit_log_queue";
    private ListOperations<String, String> listOps;

    @PostConstruct
    private void init() {
        listOps = redisTemplate.opsForList();
    }

    @Override
    @Async
    public void logAction(AuditLogRequest logRequest) {
        try {
            // Serialize DTO thành JSON và đẩy vào Redis
            String logAsJson = objectMapper.writeValueAsString(logRequest);
            listOps.leftPush(AUDIT_LOG_QUEUE_KEY, logAsJson);
        } catch (Exception e) {
            log.error("Không thể đẩy log vào Redis: {}", e.getMessage(), e);
        }
    }

    @Scheduled(fixedRate = 15000)
    @Transactional
    public void flushAuditLogs() {
        List<String> logsAsJson = listOps.range(AUDIT_LOG_QUEUE_KEY, -100, -1);
        if (logsAsJson == null || logsAsJson.isEmpty()) {
            return;
        }

        List<AuditLog> logsToSave = logsAsJson.stream().map(json -> {
            try {
                // Deserialize JSON trở lại AuditLogRequest DTO
                AuditLogRequest req = objectMapper.readValue(json, AuditLogRequest.class);

                // --- TÁI TẠO ENTITY BÊN TRONG TRANSACTION ---
                User user = userRepository.findById(req.getUserId()).orElse(null);
                Document document = req.getDocumentId() != null
                        ? documentRepository.findById(req.getDocumentId()).orElse(null)
                        : null;
                User delegatedBy = req.getDelegatedByUserId() != null
                        ? userRepository.findById(req.getDelegatedByUserId()).orElse(null)
                        : null;

                if (user == null) {
                    log.warn("Bỏ qua log vì không tìm thấy user với ID: {}", req.getUserId());
                    return null;
                }

                return AuditLog.builder()
                        .user(user)
                        .document(document)
                        .delegatedBy(delegatedBy)
                        .action(req.getAction())
                        .details(req.getDetails())
                        .ipAddress(req.getIpAddress())
                        .sessionId(req.getSessionId())
                        .build();
                // --- KẾT THÚC TÁI TẠO ---
            } catch (Exception e) {
                log.error("Lỗi deserialize AuditLog từ Redis: {}", json, e);
                return null;
            }
        }).filter(java.util.Objects::nonNull).collect(Collectors.toList());

        if (!logsToSave.isEmpty()) {
            auditLogRepository.saveAll(logsToSave);
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
}