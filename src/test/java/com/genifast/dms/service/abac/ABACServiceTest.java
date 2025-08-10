package com.genifast.dms.service.abac;

import com.genifast.dms.entity.*;
import com.genifast.dms.entity.enums.*;
import com.genifast.dms.service.abac.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * Test cho ABAC Service - Kiểm thử các kịch bản phân quyền ABAC
 * Dựa trên kịch bản trong Test_Script_DMS.markdown
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ABAC Service Tests")
class ABACServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    
    @Mock
    private ValueOperations<String, Object> valueOperations;
    
    @Mock
    private AttributeExtractor attributeExtractor;
    
    @Mock
    private PolicyEvaluator policyEvaluator;
    
    @InjectMocks
    private ABACService abacService;

    private User chuyenVienUser;
    private User truongKhoaUser;
    private User hieuTruongUser;
    private User visitorUser;
    private Document internalDocument;
    private Document privateDocument;
    private Document projectDocument;
    private Environment companyDeviceEnv;
    private Environment externalDeviceEnv;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        
        setupTestUsers();
        setupTestDocuments();
        setupTestEnvironments();
    }

    private void setupTestUsers() {
        // Chuyên viên P.DTAO
        chuyenVienUser = User.builder()
            .id(4L)
            .email("chuyenvien.dtao@genifast.edu.vn")
            .organization(Organization.builder().id(1L).build())
            .department(Department.builder().id(2L).build())
            .isAdmin(false)
            .isOrganizationManager(false)
            .isDeptManager(false)
            .currentDeviceId("device-004")
            .build();

        // Trưởng khoa K.CNTT
        truongKhoaUser = User.builder()
            .id(2L)
            .email("truongkhoa.cntt@genifast.edu.vn")
            .organization(Organization.builder().id(1L).build())
            .department(Department.builder().id(1L).build())
            .isAdmin(false)
            .isOrganizationManager(false)
            .isDeptManager(true)
            .currentDeviceId("device-002")
            .build();

        // Hiệu trưởng BGH
        hieuTruongUser = User.builder()
            .id(1L)
            .email("hieutruong@genifast.edu.vn")
            .organization(Organization.builder().id(1L).build())
            .department(Department.builder().id(3L).build())
            .isAdmin(true)
            .isOrganizationManager(true)
            .isDeptManager(false)
            .currentDeviceId("device-001")
            .build();

        // Visitor
        visitorUser = User.builder()
            .id(15L)
            .organization(null)
            .department(null)
            .isAdmin(false)
            .isOrganizationManager(false)
            .isDeptManager(false)
            .currentDeviceId("device-005")
            .build();
    }

    private void setupTestDocuments() {
        // Tài liệu INTERNAL - Quy chế tuyển sinh 2026
        internalDocument = Document.builder()
            .id(1L)
            .title("Quy chế tuyển sinh 2026")
            .organization(Organization.builder().id(1L).build())
            .department(Department.builder().id(2L).build())
            .confidentiality(DocumentConfidentiality.INTERNAL.getValue())
            .status(2) // PENDING
            .accessType(3) // INTERNAL
            .createdBy("chuyenvien.dtao@genifast.edu.vn")
            .recipients("[1, 4]") // Hiệu trưởng, Chuyên viên
            .build();

        // Tài liệu PRIVATE - Danh sách sinh viên
        privateDocument = Document.builder()
            .id(7L)
            .title("Danh sách sinh viên")
            .organization(Organization.builder().id(1L).build())
            .department(Department.builder().id(2L).build())
            .confidentiality(DocumentConfidentiality.PRIVATE.getValue())
            .status(1) // APPROVED
            .accessType(4) // PRIVATE
            .createdBy("chuyenvien.dtao@genifast.edu.vn")
            .recipients("[6]") // Phó phòng
            .build();

        // Tài liệu PROJECT - Kế hoạch DMS
        projectDocument = Document.builder()
            .id(10L)
            .title("Kế hoạch chi tiết triển khai DMS GĐ2")
            .organization(Organization.builder().id(1L).build())
            .department(Department.builder().id(1L).build())
            .project(Project.builder().id(1L).build())
            .confidentiality(DocumentConfidentiality.PROJECT.getValue())
            .status(2) // PENDING
            .accessType(5) // PROJECT
            .createdBy("truongkhoa.cntt@genifast.edu.vn")
            .recipients("[2, 4]") // Trưởng khoa, Chuyên viên
            .build();
    }

    private void setupTestEnvironments() {
        companyDeviceEnv = Environment.builder()
            .currentTime(Instant.now())
            .deviceType(DeviceType.COMPANY_DEVICE)
            .deviceId("device-004")
            .ipAddress("192.168.1.100")
            .sessionId("session-001")
            .build();

        externalDeviceEnv = Environment.builder()
            .currentTime(Instant.now())
            .deviceType(DeviceType.EXTERNAL_DEVICE)
            .deviceId("device-005")
            .ipAddress("203.162.4.100")
            .sessionId("session-002")
            .build();
    }

    @Test
    @DisplayName("Kịch bản 1.1: Chuyên viên chia sẻ tài liệu trong quyền (In-Scope)")
    void testChuyenVienShareDocumentInScope() {
        // Given
        when(valueOperations.get(anyString())).thenReturn(null); // No cache
        
        SubjectAttributes subjectAttrs = SubjectAttributes.builder()
            .userId(4L)
            .organizationId(1L)
            .departmentId(2L)
            .departmentCode("P.DTAO")
            .roles(Set.of("CHUYEN_VIEN"))
            .level(5)
            .isAdmin(false)
            .isOrganizationManager(false)
            .isDepartmentManager(false)
            .currentDeviceId("device-004")
            .build();

        ResourceAttributes resourceAttrs = ResourceAttributes.builder()
            .documentId(1L)
            .organizationId(1L)
            .departmentId(2L)
            .confidentiality(DocumentConfidentiality.INTERNAL)
            .createdBy("chuyenvien.dtao@genifast.edu.vn")
            .status(2)
            .accessType(3)
            .build();

        when(attributeExtractor.extractSubjectAttributes(chuyenVienUser)).thenReturn(subjectAttrs);
        when(attributeExtractor.extractResourceAttributes(internalDocument)).thenReturn(resourceAttrs);
        lenient().when(policyEvaluator.hasSharedPermission(4L, 1L, "documents:share:readonly")).thenReturn(true);
        when(policyEvaluator.isWithinShareTimebound(eq(4L), eq(1L), any())).thenReturn(true);

        // When
        boolean result = abacService.hasPermission(chuyenVienUser, internalDocument, "documents:share:readonly", companyDeviceEnv);

        // Then
        assertTrue(result, "Chuyên viên should be able to share document within same department");
        verify(valueOperations).set(anyString(), eq(true), any(Duration.class));
    }

    @Test
    @DisplayName("Kịch bản 1.2: Chuyên viên chia sẻ ngoài quyền (Out-of-Scope)")
    void testChuyenVienShareDocumentOutOfScope() {
        // Given
        when(valueOperations.get(anyString())).thenReturn(null); // No cache
        
        SubjectAttributes subjectAttrs = SubjectAttributes.builder()
            .userId(4L)
            .organizationId(1L)
            .departmentId(2L)
            .departmentCode("P.DTAO")
            .roles(Set.of("CHUYEN_VIEN"))
            .level(5)
            .isAdmin(false)
            .isOrganizationManager(false)
            .isDepartmentManager(false)
            .currentDeviceId("device-004")
            .build();

        ResourceAttributes resourceAttrs = ResourceAttributes.builder()
            .documentId(1L)
            .organizationId(1L)
            .departmentId(2L)
            .confidentiality(DocumentConfidentiality.INTERNAL)
            .createdBy("chuyenvien.dtao@genifast.edu.vn")
            .status(2)
            .accessType(3)
            .build();

        when(attributeExtractor.extractSubjectAttributes(chuyenVienUser)).thenReturn(subjectAttrs);
        when(attributeExtractor.extractResourceAttributes(internalDocument)).thenReturn(resourceAttrs);
        lenient().when(policyEvaluator.hasSharedPermission(4L, 1L, "documents:share:forwardable")).thenReturn(false);

        // When
        boolean result = abacService.hasPermission(chuyenVienUser, internalDocument, "documents:share:forwardable", companyDeviceEnv);

        // Then
        assertFalse(result, "Chuyên viên should NOT be able to share with forwardable permission");
        verify(valueOperations).set(anyString(), eq(false), any(Duration.class));
    }

    @Test
    @DisplayName("Kịch bản 1.3: Chuyên viên chia sẻ tài liệu PRIVATE")
    void testChuyenVienSharePrivateDocument() {
        // Given
        when(valueOperations.get(anyString())).thenReturn(null); // No cache
        
        SubjectAttributes subjectAttrs = SubjectAttributes.builder()
            .userId(4L)
            .organizationId(1L)
            .departmentId(2L)
            .departmentCode("P.DTAO")
            .roles(Set.of("CHUYEN_VIEN"))
            .level(5)
            .isAdmin(false)
            .isOrganizationManager(false)
            .isDepartmentManager(false)
            .currentDeviceId("device-004")
            .build();

        ResourceAttributes resourceAttrs = ResourceAttributes.builder()
            .documentId(7L)
            .organizationId(1L)
            .departmentId(2L)
            .confidentiality(DocumentConfidentiality.PRIVATE)
            .createdBy("chuyenvien.dtao@genifast.edu.vn")
            .status(1)
            .accessType(4)
            .build();

        when(attributeExtractor.extractSubjectAttributes(chuyenVienUser)).thenReturn(subjectAttrs);
        when(attributeExtractor.extractResourceAttributes(privateDocument)).thenReturn(resourceAttrs);
        when(policyEvaluator.hasPrivateDocAccess(4L, 7L)).thenReturn(true);
        lenient().when(policyEvaluator.hasSharedPermission(4L, 7L, "documents:share:readonly")).thenReturn(true);
        when(policyEvaluator.isWithinShareTimebound(eq(4L), eq(7L), any())).thenReturn(true);

        // When
        boolean result = abacService.hasPermission(chuyenVienUser, privateDocument, "documents:share:readonly", companyDeviceEnv);

        // Then
        assertTrue(result, "Chuyên viên should be able to share PRIVATE document if has access");
        verify(policyEvaluator).hasPrivateDocAccess(4L, 7L);
    }

    @Test
    @DisplayName("Kịch bản Device Rule: Tài liệu PRIVATE từ thiết bị ngoài bị từ chối")
    void testPrivateDocumentFromExternalDevice() {
        // Given
        when(valueOperations.get(anyString())).thenReturn(null); // No cache
        
        SubjectAttributes subjectAttrs = SubjectAttributes.builder()
            .userId(4L)
            .organizationId(1L)
            .departmentId(2L)
            .departmentCode("P.DTAO")
            .roles(Set.of("CHUYEN_VIEN"))
            .level(5)
            .isAdmin(false)
            .isOrganizationManager(false)
            .isDepartmentManager(false)
            .currentDeviceId("device-005")
            .build();

        ResourceAttributes resourceAttrs = ResourceAttributes.builder()
            .documentId(7L)
            .organizationId(1L)
            .departmentId(2L)
            .confidentiality(DocumentConfidentiality.PRIVATE)
            .createdBy("chuyenvien.dtao@genifast.edu.vn")
            .status(1)
            .accessType(4)
            .build();

        when(attributeExtractor.extractSubjectAttributes(chuyenVienUser)).thenReturn(subjectAttrs);
        when(attributeExtractor.extractResourceAttributes(privateDocument)).thenReturn(resourceAttrs);

        // When
        boolean result = abacService.hasPermission(chuyenVienUser, privateDocument, "documents:read", externalDeviceEnv);

        // Then
        assertFalse(result, "PRIVATE document should NOT be accessible from external device");
    }

    @Test
    @DisplayName("Kịch bản Project: Truy cập tài liệu dự án")
    void testProjectDocumentAccess() {
        // Given
        when(valueOperations.get(anyString())).thenReturn(null); // No cache
        
        SubjectAttributes subjectAttrs = SubjectAttributes.builder()
            .userId(2L)
            .organizationId(1L)
            .departmentId(1L)
            .departmentCode("K.CNTT")
            .roles(Set.of("TRUONG_KHOA"))
            .level(7)
            .isAdmin(false)
            .isOrganizationManager(false)
            .isDepartmentManager(true)
            .currentDeviceId("device-002")
            .build();

        ResourceAttributes resourceAttrs = ResourceAttributes.builder()
            .documentId(10L)
            .organizationId(1L)
            .departmentId(1L)
            .projectId(1L)
            .confidentiality(DocumentConfidentiality.PROJECT)
            .createdBy("truongkhoa.cntt@genifast.edu.vn")
            .status(2)
            .accessType(5)
            .build();

        when(attributeExtractor.extractSubjectAttributes(truongKhoaUser)).thenReturn(subjectAttrs);
        when(attributeExtractor.extractResourceAttributes(projectDocument)).thenReturn(resourceAttrs);
        when(policyEvaluator.isProjectMember(2L, 1L)).thenReturn(true);
        when(policyEvaluator.isProjectActive(eq(1L), any())).thenReturn(true);
        when(policyEvaluator.isWithinShareTimebound(eq(2L), eq(10L), any())).thenReturn(true);

        // When
        boolean result = abacService.hasPermission(truongKhoaUser, projectDocument, "documents:read", companyDeviceEnv);

        // Then
        assertTrue(result, "Project member should be able to access project document");
        verify(policyEvaluator, times(2)).isProjectMember(2L, 1L);
        verify(policyEvaluator).isProjectActive(eq(1L), any());
    }

    @Test
    @DisplayName("Kịch bản BGH: Hiệu trưởng có quyền xem tất cả tài liệu (trừ PRIVATE)")
    void testHieuTruongAccessAllDocuments() {
        // Given
        when(valueOperations.get(anyString())).thenReturn(null); // No cache
        
        SubjectAttributes subjectAttrs = SubjectAttributes.builder()
            .userId(1L)
            .organizationId(1L)
            .departmentId(3L)
            .departmentCode("BGH")
            .roles(Set.of("HIEU_TRUONG"))
            .level(10)
            .isAdmin(true)
            .isOrganizationManager(true)
            .isDepartmentManager(false)
            .currentDeviceId("device-001")
            .build();

        ResourceAttributes resourceAttrs = ResourceAttributes.builder()
            .documentId(1L)
            .organizationId(1L)
            .departmentId(2L) // Different department
            .confidentiality(DocumentConfidentiality.INTERNAL)
            .createdBy("chuyenvien.dtao@genifast.edu.vn")
            .status(2)
            .accessType(3)
            .build();

        when(attributeExtractor.extractSubjectAttributes(hieuTruongUser)).thenReturn(subjectAttrs);
        when(attributeExtractor.extractResourceAttributes(internalDocument)).thenReturn(resourceAttrs);
        when(policyEvaluator.isWithinShareTimebound(any(), any(), any())).thenReturn(true);

        // When
        boolean result = abacService.hasPermission(hieuTruongUser, internalDocument, "documents:read", companyDeviceEnv);

        // Then
        assertTrue(result, "Hiệu trưởng (BGH) should be able to access all INTERNAL documents");
    }

    @Test
    @DisplayName("Kịch bản Visitor: Visitor chỉ xem được tài liệu PUBLIC")
    void testVisitorAccessPublicDocument() {
        // Given
        when(valueOperations.get(anyString())).thenReturn(null); // No cache
        
        Document publicDocument = Document.builder()
            .id(6L)
            .title("Kế hoạch đào tạo 2025")
            .organization(Organization.builder().id(1L).build())
            .department(Department.builder().id(2L).build())
            .confidentiality(DocumentConfidentiality.PUBLIC.getValue())
            .status(1)
            .accessType(2)
            .build();

        SubjectAttributes subjectAttrs = SubjectAttributes.builder()
            .userId(15L)
            .organizationId(null)
            .departmentId(null)
            .departmentCode(null)
            .roles(Set.of("VISITOR"))
            .level(1)
            .isAdmin(false)
            .isOrganizationManager(false)
            .isDepartmentManager(false)
            .currentDeviceId("device-005")
            .build();

        ResourceAttributes resourceAttrs = ResourceAttributes.builder()
            .documentId(6L)
            .organizationId(1L)
            .departmentId(2L)
            .confidentiality(DocumentConfidentiality.PUBLIC)
            .status(1)
            .accessType(2)
            .build();

        when(attributeExtractor.extractSubjectAttributes(visitorUser)).thenReturn(subjectAttrs);
        when(attributeExtractor.extractResourceAttributes(publicDocument)).thenReturn(resourceAttrs);
        when(policyEvaluator.isWithinShareTimebound(eq(15L), eq(6L), any())).thenReturn(true);
        when(policyEvaluator.hasSharedPermission(15L, 6L, "documents:read")).thenReturn(true);

        // When
        boolean result = abacService.hasPermission(visitorUser, publicDocument, "documents:read", externalDeviceEnv);

        // Then
        assertTrue(result, "Visitor should be able to access PUBLIC documents");
    }

    @Test
    @DisplayName("Cache Test: Kết quả được cache và sử dụng lại")
    void testPermissionCaching() {
        // Given
        String cacheKey = "abac:permission:4:1:documents:read:COMPANY_DEVICE:" + (Instant.now().getEpochSecond() / 3600);
        when(valueOperations.get(cacheKey)).thenReturn(true);

        // When
        boolean result = abacService.hasPermission(chuyenVienUser, internalDocument, "documents:read", companyDeviceEnv);

        // Then
        assertTrue(result, "Should return cached result");
        verify(attributeExtractor, never()).extractSubjectAttributes(any());
        verify(attributeExtractor, never()).extractResourceAttributes(any());
    }

    @Test
    @DisplayName("Cache Invalidation: Xóa cache khi có thay đổi quyền user")
    void testInvalidateUserPermissions() {
        // When
        abacService.invalidateUserPermissions(4L);

        // Then
        verify(redisTemplate).keys("abac:permission:4:*");
        verify(redisTemplate).delete(any(Collection.class));
    }
}
