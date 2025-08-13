package com.genifast.dms.project;

import com.genifast.dms.entity.Department;
import com.genifast.dms.entity.Document;
import com.genifast.dms.entity.Organization;
import com.genifast.dms.entity.Project;
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
@DisplayName("Project - Quyền nâng cao")
class ProjectAccessTest {

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

    private User owner;
    private User collaborator;
    private User outsider;
    private Document projectDoc;
    private Environment envCompany;

    @BeforeEach
    void setup() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
        lenient().when(valueOps.get(anyString())).thenReturn(null);

        owner = User.builder()
            .id(201L)
            .email("owner@org.vn")
            .organization(Organization.builder().id(1L).build())
            .department(Department.builder().id(1L).build())
            .currentDeviceId("dev-owner")
            .build();

        collaborator = User.builder()
            .id(202L)
            .email("collab@org.vn")
            .organization(Organization.builder().id(1L).build())
            .department(Department.builder().id(1L).build())
            .currentDeviceId("dev-col")
            .build();

        outsider = User.builder()
            .id(203L)
            .email("outsider@org.vn")
            .organization(Organization.builder().id(1L).build())
            .department(Department.builder().id(2L).build())
            .currentDeviceId("dev-out")
            .build();

        projectDoc = Document.builder()
            .id(801L)
            .title("Tài liệu dự án P1")
            .organization(Organization.builder().id(1L).build())
            // Đặt department khác users để ép đi qua rule dự án
            .department(Department.builder().id(3L).build())
            .project(Project.builder().id(11L).build())
            .confidentiality(DocumentConfidentiality.PROJECT.getValue())
            .status(1)
            .accessType(5)
            .createdBy("owner@org.vn")
            .build();

        envCompany = Environment.builder()
            .currentTime(Instant.now())
            .deviceType(DeviceType.COMPANY_DEVICE)
            .deviceId("dev-owner")
            .ipAddress("10.0.0.5")
            .sessionId("p-1")
            .build();
    }

    private ResourceAttributes projectResource() {
        return ResourceAttributes.builder()
            .documentId(801L)
            .organizationId(1L)
            .departmentId(3L)
            .projectId(11L)
            .confidentiality(DocumentConfidentiality.PROJECT)
            .createdBy("owner@org.vn")
            .status(1)
            .accessType(5)
            .build();
    }

    private SubjectAttributes subjectOf(User u) {
        return SubjectAttributes.builder()
            .userId(u.getId())
            .organizationId(1L)
            .departmentId(u.getDepartment().getId())
            .departmentCode("DEPT_" + u.getDepartment().getId())
            .roles(Set.of("MEMBER"))
            .isAdmin(false)
            .isOrganizationManager(false)
            .isDepartmentManager(false)
            .currentDeviceId(u.getCurrentDeviceId())
            .build();
    }

    @Test
    @DisplayName("User không là thành viên dự án -> bị từ chối")
    void nonMemberDenied() {
        SubjectAttributes subject = subjectOf(outsider);
        ResourceAttributes resource = projectResource();

        when(attributeExtractor.extractSubjectAttributes(outsider)).thenReturn(subject);
        when(attributeExtractor.extractResourceAttributes(projectDoc)).thenReturn(resource);
        // Cho phép qua rule phòng ban bằng share-read để vào rule dự án
        when(policyEvaluator.hasSharedPermission(203L, 801L, "documents:read")).thenReturn(true);
        when(policyEvaluator.isProjectMember(203L, 11L)).thenReturn(false);

        boolean ok = abacService.hasPermission(outsider, projectDoc, "documents:read", envCompany);
        assertFalse(ok, "Non-member should be denied on project document");
    }

    @Test
    @DisplayName("Dự án hết hạn -> mất quyền")
    void projectExpiredLosesAccess() {
        SubjectAttributes subject = subjectOf(collaborator);
        ResourceAttributes resource = projectResource();

        when(attributeExtractor.extractSubjectAttributes(collaborator)).thenReturn(subject);
        when(attributeExtractor.extractResourceAttributes(projectDoc)).thenReturn(resource);
        // Cho phép qua rule phòng ban để kiểm tra rule dự án và thời hạn dự án
        when(policyEvaluator.hasSharedPermission(202L, 801L, "documents:read")).thenReturn(true);
        when(policyEvaluator.isProjectMember(202L, 11L)).thenReturn(true);
        // Project inactive/expired
        when(policyEvaluator.isProjectActive(eq(11L), any())).thenReturn(false);

        boolean ok = abacService.hasPermission(collaborator, projectDoc, "documents:read", envCompany);
        assertFalse(ok, "When project expired, access should be denied");
    }

    @Test
    @DisplayName("Chia sẻ trong phạm vi dự án (owner -> collaborator) thành công")
    void shareWithinProjectScope() {
        SubjectAttributes subject = subjectOf(owner);
        ResourceAttributes resource = projectResource();

        when(attributeExtractor.extractSubjectAttributes(owner)).thenReturn(subject);
        when(attributeExtractor.extractResourceAttributes(projectDoc)).thenReturn(resource);
        when(policyEvaluator.isProjectMember(201L, 11L)).thenReturn(true);
        when(policyEvaluator.isProjectActive(eq(11L), any())).thenReturn(true);
        when(policyEvaluator.hasSharedPermission(201L, 801L, "documents:share:readonly")).thenReturn(true);
        when(policyEvaluator.isWithinShareTimebound(eq(201L), eq(801L), any())).thenReturn(true);

        boolean ok = abacService.hasPermission(owner, projectDoc, "documents:share:readonly", envCompany);
        assertTrue(ok, "Owner in active project should be able to share within project scope");
    }
}
