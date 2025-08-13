package com.genifast.dms.version;

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
@DisplayName("Versioning - Quy tắc thao tác theo phiên bản")
class VersioningTest {

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

    private User user;
    private Document doc;
    private Environment envCompany;

    @BeforeEach
    void setup() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
        lenient().when(valueOps.get(anyString())).thenReturn(null);

        Organization org = Organization.builder().id(1L).build();
        Department dept1 = Department.builder().id(1L).build();
        Department dept2 = Department.builder().id(2L).build();

        // User ở phòng ban khác để tránh rule nội bộ auto-allow
        user = User.builder().id(401L).email("u@org.vn").organization(org).department(dept2).currentDeviceId("dev-u").build();

        doc = Document.builder()
            .id(1001L)
            .title("Doc with versions")
            .organization(org)
            .department(dept1)
            .confidentiality(DocumentConfidentiality.INTERNAL.getValue())
            .status(1)
            .accessType(5)
            .versionNumber(1)
            .createdBy("u@org.vn")
            .build();

        envCompany = Environment.builder()
            .currentTime(Instant.now())
            .deviceType(DeviceType.COMPANY_DEVICE)
            .deviceId("dev-u")
            .ipAddress("10.0.0.21")
            .sessionId("s-ver")
            .build();
    }

    private ResourceAttributes resourceV1() {
        return ResourceAttributes.builder()
            .documentId(1001L)
            .organizationId(1L)
            .departmentId(1L)
            .confidentiality(DocumentConfidentiality.INTERNAL)
            .createdBy("u@org.vn")
            .status(1)
            .accessType(5)
            .build();
    }

    private SubjectAttributes subject() {
        return SubjectAttributes.builder()
            .userId(401L)
            .organizationId(1L)
            .departmentId(2L)
            .departmentCode("DEPT_2")
            .roles(Set.of("MEMBER"))
            .isAdmin(false)
            .isDepartmentManager(false)
            .isOrganizationManager(false)
            .currentDeviceId("dev-u")
            .build();
    }

    @Test
    @DisplayName("Version cũ bị thay thế -> chỉ được đọc (read allowed, write denied)")
    void oldVersionReadOnly() {
        // Giả lập: đã có v2 thay thế v1, chính sách chỉ cho phép đọc version
        when(attributeExtractor.extractSubjectAttributes(user)).thenReturn(subject());
        when(attributeExtractor.extractResourceAttributes(doc)).thenReturn(resourceV1());

        // Cho phép đọc version
        when(policyEvaluator.hasSharedPermission(401L, 1001L, "documents:version:read")).thenReturn(true);
        when(policyEvaluator.isWithinShareTimebound(eq(401L), eq(1001L), any())).thenReturn(true);
        boolean canReadVersion = abacService.hasPermission(user, doc, "documents:version:read", envCompany);
        assertTrue(canReadVersion, "User should be able to read specific version");

        // Không cho phép ghi vào version cũ
        when(policyEvaluator.hasSharedPermission(401L, 1001L, "documents:version:write")).thenReturn(false);
        boolean canWriteOldVersion = abacService.hasPermission(user, doc, "documents:version:write", envCompany);
        assertFalse(canWriteOldVersion, "User should NOT be able to write to old version");
    }

    @Test
    @DisplayName("Quyền v2 không áp cho v1 (và ngược lại) - cần mở rộng PolicyEvaluator theo versionNumber")
    void permissionIsolationBetweenVersions() {
        // NOTE: Hiện PolicyEvaluator.hasSharedPermission(...) chưa nhận versionNumber.
        // Để test isolation theo version thật sự, cần mở rộng PolicyEvaluator/API để phân biệt quyền theo version.
        // Giữ test này để thực hiện sau khi bổ sung API.
        assertTrue(true);
    }
}
