package com.genifast.dms.service.abac;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.genifast.dms.entity.*;
import com.genifast.dms.entity.enums.DocumentConfidentiality;
import com.genifast.dms.service.abac.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Trích xuất thuộc tính từ các entity để sử dụng trong ABAC
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AttributeExtractor {

    private final ObjectMapper objectMapper;

    /**
     * Trích xuất thuộc tính của Subject (User)
     */
    public SubjectAttributes extractSubjectAttributes(User user) {
        try {
            // Trích xuất roles từ user_roles
            Set<String> roles = user.getUserRoles() != null ? 
                user.getUserRoles().stream()
                    .map(role -> role.getName())
                    .collect(Collectors.toSet()) : 
                Collections.emptySet();

            return SubjectAttributes.builder()
                .userId(user.getId())
                .organizationId(user.getOrganizationId())
                .departmentId(user.getDeptId())
                .departmentCode(user.getDepartment() != null ? user.getDepartment().getName() : null)
                .roles(roles)
                .level(determineUserLevel(user))
                .isAdmin(user.getIsAdmin())
                .isOrganizationManager(user.getIsOrganizationManager())
                .isDepartmentManager(user.getIsDeptManager())
                .currentDeviceId(user.getCurrentDeviceId())
                .build();
        } catch (Exception e) {
            log.error("Error extracting subject attributes for user {}", user.getId(), e);
            throw new RuntimeException("Failed to extract subject attributes", e);
        }
    }

    /**
     * Trích xuất thuộc tính của Resource (Document)
     */
    public ResourceAttributes extractResourceAttributes(Document document) {
        try {
            // Parse recipients từ JSON string
            List<Long> recipients = parseRecipients(document.getRecipients());

            // Xác định confidentiality
            DocumentConfidentiality confidentiality = document.getConfidentiality() != null ?
                DocumentConfidentiality.fromValue(document.getConfidentiality()) :
                DocumentConfidentiality.INTERNAL;

            return ResourceAttributes.builder()
                .documentId(document.getId())
                .organizationId(document.getOrganizationId())
                .departmentId(document.getDeptId())
                .projectId(document.getProjectId())
                .documentType(document.getType())
                .confidentiality(confidentiality)
                .createdBy(document.getCreatedBy())
                .createdDate(document.getCreatedAt())
                .recipients(recipients)
                .status(document.getStatus())
                .accessType(document.getAccessType())
                .build();
        } catch (Exception e) {
            log.error("Error extracting resource attributes for document {}", document.getId(), e);
            throw new RuntimeException("Failed to extract resource attributes", e);
        }
    }

    /**
     * Xác định level của user dựa trên role và position
     */
    private Integer determineUserLevel(User user) {
        if (user.getIsAdmin()) return 10;
        if (user.getIsOrganizationManager()) return 9;
        if (user.getIsDeptManager()) return 8;
        
        // Có thể mở rộng logic dựa trên role cụ thể
        Set<String> roles = user.getUserRoles() != null ? 
            user.getUserRoles().stream()
                .map(role -> role.getName())
                .collect(Collectors.toSet()) : 
            Collections.emptySet();
        
        if (roles.contains("HIEU_TRUONG")) return 10;
        if (roles.contains("TRUONG_KHOA")) return 7;
        if (roles.contains("PHO_KHOA")) return 6;
        if (roles.contains("PHO_PHONG")) return 6;
        if (roles.contains("CHUYEN_VIEN")) return 5;
        if (roles.contains("GIAO_VU")) return 4;
        if (roles.contains("CAN_BO")) return 3;
        if (roles.contains("VAN_THU")) return 3;
        if (roles.contains("PHAP_CHE")) return 7;
        if (roles.contains("NHAN_VIEN_LUU_TRU")) return 4;
        if (roles.contains("NGUOI_NHAN")) return 2;
        if (roles.contains("VISITOR")) return 1;
        
        return 2; // Default level
    }

    /**
     * Parse recipients từ JSON string
     */
    private List<Long> parseRecipients(String recipientsJson) {
        if (recipientsJson == null || recipientsJson.trim().isEmpty()) {
            return Collections.emptyList();
        }
        
        try {
            return objectMapper.readValue(recipientsJson, new TypeReference<List<Long>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse recipients JSON: {}", recipientsJson, e);
            return Collections.emptyList();
        }
    }
}
