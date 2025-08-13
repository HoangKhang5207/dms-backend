package com.genifast.dms.abac;

import com.genifast.dms.entity.Department;
import com.genifast.dms.entity.Document;
import com.genifast.dms.entity.Organization;
import com.genifast.dms.entity.User;
import com.genifast.dms.entity.enums.DeviceType;
import com.genifast.dms.entity.enums.DocumentConfidentiality;
import com.genifast.dms.service.abac.ABACService;
import com.genifast.dms.service.abac.AttributeExtractor;
import com.genifast.dms.service.abac.PolicyEvaluator;
import com.genifast.dms.service.abac.model.Environment;
import com.genifast.dms.service.abac.model.ResourceAttributes;
import com.genifast.dms.service.abac.model.SubjectAttributes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Instant;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ABAC - Phòng ban phối hợp")
class AbacCooperationTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    @Mock
    private ValueOperations<String, Object> valueOps;
    @Mock
    private AttributeExtractor attributeExtractor;
    @Mock
    private PolicyEvaluator policyEvaluator;
    @InjectMocks
    private ABACService abacService;

    private User userDeptA;
    private Document docDeptB_Internal;
    private Environment envCompany;
    private Environment envExternal;

    @BeforeEach
    void init() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
        lenient().when(valueOps.get(anyString())).thenReturn(null);

        userDeptA = User.builder()
            .id(101L)
            .email("userA@org.vn")
            .organization(Organization.builder().id(1L).build())
            .department(Department.builder().id(10L).build())
            .currentDeviceId("dev-A")
            .build();

        docDeptB_Internal = Document.builder()
            .id(501L)
            .title("VB nội bộ phòng B")
            .organization(Organization.builder().id(1L).build())
            .department(Department.builder().id(20L).build())
            .confidentiality(DocumentConfidentiality.INTERNAL.getValue())
            .status(1)
            .accessType(3)
            .createdBy("ownerB@org.vn")
            .build();

        envCompany = Environment.builder()
            .currentTime(Instant.now())
            .deviceType(DeviceType.COMPANY_DEVICE)
            .deviceId("dev-A")
            .ipAddress("10.0.0.1")
            .sessionId("s-1")
            .build();

        envExternal = Environment.builder()
            .currentTime(Instant.now())
            .deviceType(DeviceType.EXTERNAL_DEVICE)
            .deviceId("ext-1")
            .ipAddress("203.0.113.10")
            .sessionId("s-2")
            .build();
    }

    @Test
    @DisplayName("Readonly share liên phòng ban (A -> tài liệu phòng B) được phép theo policy")
    void readonlyShareBetweenDepartmentsAllowedByPolicy() {
        SubjectAttributes subject = SubjectAttributes.builder()
            .userId(101L)
            .organizationId(1L)
            .departmentId(10L)
            .departmentCode("PHONG_A")
            .roles(Set.of("CHUYEN_VIEN"))
            .isAdmin(false)
            .isOrganizationManager(false)
            .isDepartmentManager(false)
            .currentDeviceId("dev-A")
            .build();

        ResourceAttributes resource = ResourceAttributes.builder()
            .documentId(501L)
            .organizationId(1L)
            .departmentId(20L)
            .confidentiality(DocumentConfidentiality.INTERNAL)
            .createdBy("ownerB@org.vn")
            .status(1)
            .accessType(3)
            .build();

        when(attributeExtractor.extractSubjectAttributes(userDeptA)).thenReturn(subject);
        when(attributeExtractor.extractResourceAttributes(docDeptB_Internal)).thenReturn(resource);
        when(policyEvaluator.hasSharedPermission(101L, 501L, "documents:share:readonly")).thenReturn(true);
        when(policyEvaluator.isWithinShareTimebound(eq(101L), eq(501L), any())).thenReturn(true);

        boolean ok = abacService.hasPermission(userDeptA, docDeptB_Internal, "documents:share:readonly", envCompany);
        assertTrue(ok, "Cross-dept readonly share should be allowed when policy grants it");
    }

    @Test
    @DisplayName("Override chia sẻ liên phòng ban: thiếu lý do -> bị từ chối")
    void overrideWithoutBusinessReasonDenied() {
        SubjectAttributes subject = SubjectAttributes.builder()
            .userId(101L)
            .organizationId(1L)
            .departmentId(10L)
            .departmentCode("PHONG_A")
            .roles(Set.of("CHUYEN_VIEN"))
            .isAdmin(false)
            .isOrganizationManager(false)
            .isDepartmentManager(false)
            .currentDeviceId("dev-A")
            .build();

        ResourceAttributes resource = ResourceAttributes.builder()
            .documentId(501L)
            .organizationId(1L)
            .departmentId(20L)
            .confidentiality(DocumentConfidentiality.INTERNAL)
            .createdBy("ownerB@org.vn")
            .status(1)
            .accessType(3)
            .build();

        when(attributeExtractor.extractSubjectAttributes(userDeptA)).thenReturn(subject);
        when(attributeExtractor.extractResourceAttributes(docDeptB_Internal)).thenReturn(resource);
        // Mô phỏng thiếu lý do -> policy không cho
        when(policyEvaluator.hasSharedPermission(101L, 501L, "documents:share:override")).thenReturn(false);

        boolean ok = abacService.hasPermission(userDeptA, docDeptB_Internal, "documents:share:override", envCompany);
        assertFalse(ok, "Override without business reason should be denied by policy");
    }

    @Test
    @DisplayName("Override chia sẻ liên phòng ban: có lý do hợp lệ -> được phép")
    void overrideWithBusinessReasonAllowed() {
        SubjectAttributes subject = SubjectAttributes.builder()
            .userId(101L)
            .organizationId(1L)
            .departmentId(10L)
            .departmentCode("PHONG_A")
            .roles(Set.of("CHUYEN_VIEN"))
            .isAdmin(false)
            .isOrganizationManager(false)
            .isDepartmentManager(false)
            .currentDeviceId("dev-A")
            .build();

        ResourceAttributes resource = ResourceAttributes.builder()
            .documentId(501L)
            .organizationId(1L)
            .departmentId(20L)
            .confidentiality(DocumentConfidentiality.INTERNAL)
            .createdBy("ownerB@org.vn")
            .status(1)
            .accessType(3)
            .build();

        when(attributeExtractor.extractSubjectAttributes(userDeptA)).thenReturn(subject);
        when(attributeExtractor.extractResourceAttributes(docDeptB_Internal)).thenReturn(resource);
        // Mô phỏng có lý do -> dùng biến thể action thể hiện đã kèm lý do
        when(policyEvaluator.hasSharedPermission(101L, 501L, "documents:share:override:reason")).thenReturn(true);
        when(policyEvaluator.isWithinShareTimebound(eq(101L), eq(501L), any())).thenReturn(true);

        boolean ok = abacService.hasPermission(userDeptA, docDeptB_Internal, "documents:share:override:reason", envCompany);
        assertTrue(ok, "Override with valid business reason should be allowed by policy");
    }

    @Test
    @DisplayName("Device rule: Truy cập tài liệu INTERNAL từ thiết bị ngoài -> bị từ chối")
    void deviceRuleExternalDenied() {
        SubjectAttributes subject = SubjectAttributes.builder()
            .userId(101L)
            .organizationId(1L)
            .departmentId(10L)
            .departmentCode("PHONG_A")
            .roles(Set.of("CHUYEN_VIEN"))
            .isAdmin(false)
            .isOrganizationManager(false)
            .isDepartmentManager(false)
            .currentDeviceId("dev-A")
            .build();

        ResourceAttributes resource = ResourceAttributes.builder()
            .documentId(501L)
            .organizationId(1L)
            .departmentId(20L)
            .confidentiality(DocumentConfidentiality.INTERNAL)
            .createdBy("ownerB@org.vn")
            .status(1)
            .accessType(3)
            .build();

        when(attributeExtractor.extractSubjectAttributes(userDeptA)).thenReturn(subject);
        when(attributeExtractor.extractResourceAttributes(docDeptB_Internal)).thenReturn(resource);
        // Policy có thể cho, nhưng device rule sẽ chặn vì thiết bị ngoài
        lenient().when(policyEvaluator.hasSharedPermission(101L, 501L, "documents:read")).thenReturn(true);

        boolean ok = abacService.hasPermission(userDeptA, docDeptB_Internal, "documents:read", envExternal);
        assertFalse(ok, "INTERNAL document should be denied on external device by device rule");
    }
}
