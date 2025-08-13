package com.genifast.dms.service.impl;

import com.genifast.dms.common.exception.ApiException;
import com.genifast.dms.common.utils.JwtUtils;
import com.genifast.dms.dto.request.DelegationRequest;
import com.genifast.dms.dto.response.DelegationResponse;
import com.genifast.dms.entity.Delegation;
import com.genifast.dms.entity.Document;
import com.genifast.dms.entity.User;
import com.genifast.dms.repository.DelegationRepository;
import com.genifast.dms.repository.DocumentRepository;
import com.genifast.dms.repository.UserRepository;
import com.genifast.dms.config.CustomPermissionEvaluator;
import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Transactional
@DisplayName("DelegationServiceImpl Tests")
class DelegationServiceImplTest {

    @Autowired
    private DelegationServiceImpl delegationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private DelegationRepository delegationRepository;

    @MockBean
    private CustomPermissionEvaluator permissionEvaluator;

    private User delegator;
    private User delegatee;
    private Document document;

    private MockedStatic<JwtUtils> jwtUtilsMock;

    @BeforeEach
    void setup() {
        delegationRepository.deleteAll();
        documentRepository.deleteAll();
        userRepository.deleteAll();

        String uid = java.util.UUID.randomUUID().toString().substring(0, 8);

        delegator = userRepository.save(User.builder()
                .firstName("Hieu")
                .lastName("Truong")
                .fullName("Hieu Truong")
                .email("hieutruong+" + uid + "@genifast.edu.vn")
                .isAdmin(false)
                .build());

        delegatee = userRepository.save(User.builder()
                .firstName("Pho")
                .lastName("Khoa")
                .fullName("Pho Khoa")
                .email("pho.khoa+" + uid + "@genifast.edu.vn")
                .isAdmin(false)
                .build());

        document = documentRepository.save(Document.builder()
                .title("Quyết định nội bộ")
                .content("Nội dung mô tả")
                .type("pdf")
                .originalFilename("qd.pdf")
                .status(1)
                .accessType(2) // INTERNAL
                .build());

        jwtUtilsMock = org.mockito.Mockito.mockStatic(JwtUtils.class);
        jwtUtilsMock.when(JwtUtils::getCurrentUserLogin).thenReturn(java.util.Optional.of(delegator.getEmail()));
    }

    @AfterEach
    void teardown() {
        if (jwtUtilsMock != null) jwtUtilsMock.close();
    }

    private DelegationRequest newReq() {
        DelegationRequest req = new DelegationRequest();
        req.setDelegateeId(delegatee.getId());
        req.setDocumentId(document.getId());
        req.setPermission("documents:approve");
        req.setStartAt(Instant.now().minusSeconds(60));
        req.setExpiryAt(Instant.now().plusSeconds(3600));
        return req;
    }

    @Test
    @WithMockUser(username = "hieutruong@genifast.edu.vn", authorities = {"delegate_process"})
    @DisplayName("Create delegation: thành công khi delegator có quyền trên tài liệu")
    void createDelegation_Success() {
        when(permissionEvaluator.hasPermission(any(), any(Long.class), any(String.class))).thenReturn(true);

        DelegationResponse res = delegationService.createDelegation(newReq());
        assertThat(res).isNotNull();
        assertThat(res.getDelegateeId()).isEqualTo(delegatee.getId());

        Delegation saved = delegationRepository.findAll().stream().findFirst().orElse(null);
        assertThat(saved).isNotNull();
        assertThat(saved.getPermission()).isEqualTo("documents:approve");
    }

    @Test
    @WithMockUser(username = "hieutruong@genifast.edu.vn", authorities = {"delegate_process"})
    @DisplayName("Create delegation: bị chặn khi delegator KHÔNG có quyền trên tài liệu")
    void createDelegation_DelegatorNoPermission_Throws() {
        when(permissionEvaluator.hasPermission(any(), any(Long.class), any(String.class))).thenReturn(false);
        assertThrows(ApiException.class, () -> delegationService.createDelegation(newReq()));
    }

    @Test
    @WithMockUser(username = "hieutruong@genifast.edu.vn", authorities = {"delegate_process"})
    @DisplayName("Create delegation: invalid khi tự ủy quyền cho chính mình")
    void createDelegation_Self_Invalid() {
        // set delegatee = delegator
        delegatee = delegator;
        DelegationRequest req = newReq();
        req.setDelegateeId(delegator.getId());
        when(permissionEvaluator.hasPermission(any(), any(Long.class), any(String.class))).thenReturn(true);
        assertThrows(ApiException.class, () -> delegationService.createDelegation(req));
    }

    @Test
    @WithMockUser(username = "hieutruong@genifast.edu.vn", authorities = {"delegate_process"})
    @DisplayName("Revoke delegation: delegator thu hồi thành công")
    void revokeDelegation_ByDelegator_Success() {
        when(permissionEvaluator.hasPermission(any(), any(Long.class), any(String.class))).thenReturn(true);
        DelegationResponse res = delegationService.createDelegation(newReq());
        Long delegationId = delegationRepository.findAll().get(0).getId();

        delegationService.revokeDelegation(delegationId);
        assertThat(delegationRepository.findById(delegationId)).isEmpty();
    }

    @Test
    @WithMockUser(username = "pho.khoa@genifast.edu.vn", authorities = {"delegate_process"})
    @DisplayName("Revoke delegation: người khác thu hồi -> FORBIDDEN")
    void revokeDelegation_ByOther_Forbidden() {
        when(permissionEvaluator.hasPermission(any(), any(Long.class), any(String.class))).thenReturn(true);
        DelegationResponse res = delegationService.createDelegation(newReq());
        Long delegationId = delegationRepository.findAll().get(0).getId();

        // giả lập current user là delegatee (không phải delegator, không phải admin)
        jwtUtilsMock.when(JwtUtils::getCurrentUserLogin).thenReturn(java.util.Optional.of(delegatee.getEmail()));
        assertThrows(ApiException.class, () -> delegationService.revokeDelegation(delegationId));
    }

    @Test
    @WithMockUser(username = "hieutruong@genifast.edu.vn", authorities = {"documents:read", "delegate_process"})
    @DisplayName("List delegations by document: trả về danh sách")
    void getDelegationsByDocument_ReturnsList() {
        when(permissionEvaluator.hasPermission(any(), any(Long.class), any(String.class))).thenReturn(true);
        delegationService.createDelegation(newReq());

        assertThat(delegationService.getDelegationsByDocument(document.getId())).hasSize(1);
    }
}
