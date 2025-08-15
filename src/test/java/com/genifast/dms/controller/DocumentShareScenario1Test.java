package com.genifast.dms.controller;

import com.genifast.dms.dto.request.DocumentShareRequest;
import com.genifast.dms.service.DocumentService;
import com.genifast.dms.service.FileStorageService;
import com.genifast.dms.service.util.WatermarkService;
import com.genifast.dms.dto.request.AuditLogRequest;
import com.genifast.dms.entity.User;
import com.genifast.dms.testutil.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static com.genifast.dms.testutil.TestData.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("DocumentController - Share Scenario 1")
class DocumentShareScenario1Test {

    private MockMvc mockMvc;

    @Mock
    private DocumentService documentService;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private WatermarkService watermarkService;

    @Mock
    private com.genifast.dms.service.AuditLogService auditLogService;

    @Mock
    private com.genifast.dms.repository.UserRepository userRepository;

    @BeforeEach
    void setup() {
        DocumentController controller = new DocumentController(documentService, fileStorageService, watermarkService);
        com.genifast.dms.common.handler.GlobalExceptionHandler advice =
                new com.genifast.dms.common.handler.GlobalExceptionHandler(auditLogService, userRepository);
        this.mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(advice)
                .build();
    }

    private void setAuthenticatedUser(String email, Long userId) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(email, "N/A"));
        User mockUser = User.builder().id(userId).email(email).build();
        org.mockito.Mockito.when(userRepository.findByEmail(email)).thenReturn(java.util.Optional.of(mockUser));
    }

    // =============================
    // KỊCH BẢN 1: Chuyên viên (user-cv)
    //  - [1.1] In-Scope: share readonly trong tổ chức -> 201
    //  - [1.2] Out-of-Scope: share forwardable vượt quyền -> 403
    //  - [1.3] Share PRIVATE với timebound hợp lệ -> 201
    //  - [1.4] Share PRIVATE cho người không thuộc private_docs -> 403
    // =============================

    // [1.1] theo ai/kich ban.md - Chia sẻ readonly trong tổ chức (In-Scope) -> 201 Created
    @Test
    @DisplayName("1.1 - POST /api/v1/documents/{id}/share - internal readonly - 201 Created")
    void scenario11_InternalReadonly_Created() throws Exception {
        Long docId = DOC_INTERNAL_01; // đại diện doc-01 (INTERNAL, PENDING)
        String body = TestData.buildShareBody(
                USER_GV,
                List.of(PERM_READONLY),
                null,
                null,
                null
        );

        doNothing().when(documentService).shareDocument(eq(docId), any(DocumentShareRequest.class));

        mockMvc.perform(post("/api/v1/documents/{id}/share", docId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Tài liệu đã được chia sẻ thành công."));

        ArgumentCaptor<DocumentShareRequest> captor = ArgumentCaptor.forClass(DocumentShareRequest.class);
        verify(documentService).shareDocument(eq(docId), captor.capture());
        DocumentShareRequest sent = captor.getValue();
        assertThat(sent.getRecipientId()).isEqualTo(USER_GV);
        assertThat(sent.getPermissions()).containsExactly(PERM_READONLY);
        assertThat(Boolean.TRUE.equals(sent.getIsShareToExternal())).isFalse();
    }

    // [1.2] theo ai/kich ban.md - Chia sẻ forwardable vượt quyền (Out-of-Scope) -> 403 Forbidden
    @Test
    @DisplayName("1.2 - POST /api/v1/documents/{id}/share - forwardable out-of-scope - 403 Forbidden")
    void scenario12_Forwardable_OutOfScope_Forbidden() throws Exception {
        Long docId = DOC_INTERNAL_01_ALT; // vẫn là doc-01
        String body = TestData.buildShareBody(
                USER_GV,
                List.of(PERM_FORWARDABLE),
                null,
                null,
                false
        );

        setAuthenticatedUser("user-cv@example.com", 1101L);
        doThrow(new AccessDeniedException("Permission documents:share:forwardable is not allowed."))
                .when(documentService).shareDocument(eq(docId), any(DocumentShareRequest.class));

        mockMvc.perform(post("/api/v1/documents/{id}/share", docId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("Bạn không có quyền truy cập hay thực hiện hành động này."))
                .andExpect(jsonPath("$.path").value("/api/v1/documents/" + docId + "/share"));

        // verify audit log called
        org.mockito.ArgumentCaptor<AuditLogRequest> logCaptor = org.mockito.ArgumentCaptor.forClass(AuditLogRequest.class);
        org.mockito.Mockito.verify(auditLogService).logAction(logCaptor.capture());
        AuditLogRequest logReq = logCaptor.getValue();
        org.assertj.core.api.Assertions.assertThat(logReq.getAction()).isEqualTo("ACCESS_DENIED");
        org.assertj.core.api.Assertions.assertThat(logReq.getUserId()).isEqualTo(1101L);
    }

    // [1.3] theo ai/kich ban.md - Chia sẻ tài liệu PRIVATE với timebound hợp lệ -> 201 Created
    @Test
    @DisplayName("1.3 - POST /api/v1/documents/{id}/share - PRIVATE readonly timebound - 201 Created")
    void scenario13_Private_Timebound_Created() throws Exception {
        Long docId = DOC_PRIVATE_07; // đại diện doc-07 (PRIVATE, APPROVED)
        String body = TestData.buildShareBody(
                USER_PP,
                List.of(PERM_READONLY, PERM_TIMEBOUND),
                TIME_FROM_SAMPLE,
                TIME_TO_SAMPLE,
                null
        );

        doNothing().when(documentService).shareDocument(eq(docId), any(DocumentShareRequest.class));

        mockMvc.perform(post("/api/v1/documents/{id}/share", docId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Tài liệu đã được chia sẻ thành công."));

        ArgumentCaptor<DocumentShareRequest> captor = ArgumentCaptor.forClass(DocumentShareRequest.class);
        verify(documentService).shareDocument(eq(docId), captor.capture());
        DocumentShareRequest sent = captor.getValue();
        assertThat(sent.getRecipientId()).isEqualTo(USER_PP);
        assertThat(sent.getPermissions()).contains(PERM_READONLY, PERM_TIMEBOUND);
        assertThat(sent.getFromDate()).isNotNull();
        assertThat(sent.getToDate()).isNotNull();
        assertThat(Boolean.TRUE.equals(sent.getIsShareToExternal())).isFalse();
    }

    // [1.4] theo ai/kich ban.md - Chia sẻ PRIVATE cho người không thuộc private_docs -> 403 Forbidden
    @Test
    @DisplayName("1.4 - POST /api/v1/documents/{id}/share - PRIVATE recipient not in private_docs - 403 Forbidden")
    void scenario14_Private_RecipientNotInPrivateDocs_Forbidden() throws Exception {
        Long docId = DOC_PRIVATE_07_ALT; // doc-07 negative
        String body = TestData.buildShareBody(
                USER_CB, // không thuộc private_docs
                List.of(PERM_READONLY),
                null,
                null,
                null
        );

        setAuthenticatedUser("user-cv@example.com", 1101L);
        doThrow(new AccessDeniedException("Recipient is not allowed to access private document."))
                .when(documentService).shareDocument(eq(docId), any(DocumentShareRequest.class));

        mockMvc.perform(post("/api/v1/documents/{id}/share", docId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("Bạn không có quyền truy cập hay thực hiện hành động này."))
                .andExpect(jsonPath("$.path").value("/api/v1/documents/" + docId + "/share"));

        // verify audit log called
        org.mockito.ArgumentCaptor<AuditLogRequest> logCaptor = org.mockito.ArgumentCaptor.forClass(AuditLogRequest.class);
        org.mockito.Mockito.verify(auditLogService).logAction(logCaptor.capture());
        AuditLogRequest logReq = logCaptor.getValue();
        org.assertj.core.api.Assertions.assertThat(logReq.getAction()).isEqualTo("ACCESS_DENIED");
        org.assertj.core.api.Assertions.assertThat(logReq.getUserId()).isEqualTo(1101L);
    }
}

