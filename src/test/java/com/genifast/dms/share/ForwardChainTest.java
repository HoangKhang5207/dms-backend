package com.genifast.dms.share;

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
@DisplayName("Share - Forward chain")
class ForwardChainTest {

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

    private User userA;
    private User userB;
    private User userC;
    private Document doc;
    private Environment envCompany;

    @BeforeEach
    void setup() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
        lenient().when(valueOps.get(anyString())).thenReturn(null);

        Organization org = Organization.builder().id(1L).build();
        Department dept1 = Department.builder().id(1L).build();
        Department dept2 = Department.builder().id(2L).build();

        // userA cũng ở phòng ban khác với document để ép đi qua policy share/forward
        userA = User.builder().id(301L).email("a@org.vn").organization(org).department(dept2).currentDeviceId("dev-a").build();
        // userB ở phòng ban khác để tránh rule nội bộ auto-allow
        userB = User.builder().id(302L).email("b@org.vn").organization(org).department(dept2).currentDeviceId("dev-b").build();
        userC = User.builder().id(303L).email("c@org.vn").organization(org).department(dept2).currentDeviceId("dev-c").build();

        doc = Document.builder()
            .id(901L)
            .title("Doc forward")
            .organization(org)
            .department(dept1)
            .confidentiality(DocumentConfidentiality.INTERNAL.getValue())
            .status(1)
            .accessType(5)
            .createdBy("a@org.vn")
            .build();

        envCompany = Environment.builder()
            .currentTime(Instant.now())
            .deviceType(DeviceType.COMPANY_DEVICE)
            .deviceId("dev-a")
            .ipAddress("10.0.0.11")
            .sessionId("s-fwd")
            .build();
    }

    private ResourceAttributes resource() {
        return ResourceAttributes.builder()
            .documentId(901L)
            .organizationId(1L)
            .departmentId(1L)
            .confidentiality(DocumentConfidentiality.INTERNAL)
            .createdBy("a@org.vn")
            .status(1)
            .accessType(5)
            .build();
    }

    private SubjectAttributes subject(User u) {
        Long depId = u.getDepartment() != null ? u.getDepartment().getId() : null;
        String depCode = depId != null ? ("DEPT_" + depId) : null;
        return SubjectAttributes.builder()
            .userId(u.getId())
            .organizationId(1L)
            .departmentId(depId)
            .departmentCode(depCode)
            .roles(Set.of("MEMBER"))
            .isAdmin(false)
            .isDepartmentManager(false)
            .isOrganizationManager(false)
            .currentDeviceId(u.getCurrentDeviceId())
            .build();
    }

    @Test
    @DisplayName("Cascading share: A->B được phép, B->C bị chặn nếu không có forward permission")
    void cascadingShare() {
        ResourceAttributes res = resource();

        // A -> B: action share readonly
        when(attributeExtractor.extractSubjectAttributes(userA)).thenReturn(subject(userA));
        when(attributeExtractor.extractResourceAttributes(doc)).thenReturn(res);
        when(policyEvaluator.hasSharedPermission(301L, 901L, "documents:share:readonly")).thenReturn(true);
        when(policyEvaluator.isWithinShareTimebound(eq(301L), eq(901L), any())).thenReturn(true);

        boolean a2b = abacService.hasPermission(userA, doc, "documents:share:readonly", envCompany);
        assertTrue(a2b, "A should be able to share to B");

        // B -> C: need explicit forward permission for B on that doc
        when(attributeExtractor.extractSubjectAttributes(userB)).thenReturn(subject(userB));
        when(attributeExtractor.extractResourceAttributes(doc)).thenReturn(res);
        when(policyEvaluator.hasSharedPermission(302L, 901L, "documents:forward")).thenReturn(false);

        boolean b2c = abacService.hasPermission(userB, doc, "documents:forward", envCompany);
        assertFalse(b2c, "B should not be able to forward to C without forward permission");
    }

    @Test
    @DisplayName("Thu hồi tại A làm mất hiệu lực B/C (timebound/permission revoked)")
    void revokeCascade() {
        ResourceAttributes res = resource();

        // A -> B: initially allowed
        when(attributeExtractor.extractSubjectAttributes(userA)).thenReturn(subject(userA));
        when(attributeExtractor.extractResourceAttributes(doc)).thenReturn(res);
        when(policyEvaluator.hasSharedPermission(301L, 901L, "documents:share:readonly")).thenReturn(true);
        when(policyEvaluator.isWithinShareTimebound(eq(301L), eq(901L), any())).thenReturn(true);
        assertTrue(abacService.hasPermission(userA, doc, "documents:share:readonly", envCompany));

        // Revoke at A: permission removed (đủ để fail sớm ở rule phòng ban)
        when(policyEvaluator.hasSharedPermission(301L, 901L, "documents:share:readonly")).thenReturn(false);

        boolean a2bAfterRevoke = abacService.hasPermission(userA, doc, "documents:share:readonly", envCompany);
        assertFalse(a2bAfterRevoke, "After revoke, A cannot continue to share");

        // B attempting to access/read after revoke should also fail due to department rule + no share grant
        when(attributeExtractor.extractSubjectAttributes(userB)).thenReturn(subject(userB));
        when(attributeExtractor.extractResourceAttributes(doc)).thenReturn(res);
        // No shared permission from policy to allow B anymore (fail sớm tại rule phòng ban)
        when(policyEvaluator.hasSharedPermission(302L, 901L, "documents:read")).thenReturn(false);

        boolean bRead = abacService.hasPermission(userB, doc, "documents:read", envCompany);
        assertFalse(bRead, "B should lose access after A revoked");
    }
}
