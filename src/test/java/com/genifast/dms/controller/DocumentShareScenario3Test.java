package com.genifast.dms.controller;

// import com.genifast.dms.common.constant.ErrorCode; // removed unused
import com.genifast.dms.dto.request.AuditLogRequest;
import com.genifast.dms.entity.User;
import com.genifast.dms.dto.request.DocumentShareRequest;
import com.genifast.dms.service.DocumentService;
import com.genifast.dms.service.FileStorageService;
import com.genifast.dms.service.util.WatermarkService;
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

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static com.genifast.dms.testutil.TestData.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("DocumentController - Share Scenario 3 (Hiệu trưởng)")
class DocumentShareScenario3Test {

    private MockMvc mockMvc;

    @Mock private DocumentService documentService;
    @Mock private FileStorageService fileStorageService;
    @Mock private WatermarkService watermarkService;
    @Mock private com.genifast.dms.service.AuditLogService auditLogService;
    @Mock private com.genifast.dms.repository.UserRepository userRepository;

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

    // [3.1] theo ai/kich ban.md - Chia sẻ ra ngoài tổ chức (readonly) -> 201 Created
    @Test
    @DisplayName("3.1 - POST /api/v1/documents/{id}/share - external readonly - 201 Created")
    void scenario31_ExternalReadonly_Created() throws Exception {
        Long docId = DOC_SC3_EXTERNAL_READONLY;
        String body = TestData.buildShareBody(
                USER_EXTERNAL,
                List.of(PERM_READONLY),
                null,
                null,
                true
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
        assertThat(sent.getRecipientId()).isEqualTo(USER_EXTERNAL);
        assertThat(sent.getPermissions()).containsExactly(PERM_READONLY);
        assertThat(Boolean.TRUE.equals(sent.getIsShareToExternal())).isTrue();
    }

    // [3.2] theo ai/kich ban.md - Chia sẻ với quyền shareable -> 201 Created
    @Test
    @DisplayName("3.2 - POST /api/v1/documents/{id}/share - shareable - 201 Created")
    void scenario32_Shareable_Created() throws Exception {
        Long docId = DOC_SC3_SHAREABLE;
        String body = TestData.buildShareBody(
                USER_SC3,
                List.of(PERM_SHAREABLE),
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
        assertThat(sent.getRecipientId()).isEqualTo(USER_SC3);
        assertThat(sent.getPermissions()).containsExactly(PERM_SHAREABLE);
    }

    // [3.3] theo ai/kich ban.md - Chia sẻ cho người nhận không hoạt động -> 403 Forbidden
    @Test
    @DisplayName("3.3 - POST /api/v1/documents/{id}/share - recipient inactive - 403 Forbidden")
    void scenario33_RecipientInactive_Forbidden() throws Exception {
        Long docId = DOC_SC3_RECIPIENT_INACTIVE;
        String body = TestData.buildShareBody(
                USER_INACTIVE,
                List.of(PERM_READONLY),
                null,
                null,
                null
        );

        setAuthenticatedUser("user-ht@example.com", 1301L);
        doThrow(new AccessDeniedException("Recipient is inactive."))
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
        org.assertj.core.api.Assertions.assertThat(logReq.getUserId()).isEqualTo(1301L);
    }

    // [3.4] theo ai/kich ban.md - Tạo liên kết công khai qua /share-public (allowDownload=false)
    @Test
    @DisplayName("3.4 - POST /api/v1/documents/{id}/share-public - allowDownload=false - 201 Created")
    void scenario34_CreatePublicLink_DisallowDownload_Created() throws Exception {
        Long docId = 304L;
        Instant expiryAt = Instant.parse("2025-12-31T23:59:59Z");
        when(documentService.createShareLink(eq(docId), eq(expiryAt), eq(false)))
                .thenReturn("https://example.com/share/public/" + docId + "?dl=0");

        mockMvc.perform(post("/api/v1/documents/{id}/share-public", docId)
                        .param("expiryAt", expiryAt.toString())
                        .param("allowDownload", "false"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.link").value("https://example.com/share/public/" + docId + "?dl=0"));
    }

    // [3.4] (phụ) theo ai/kich ban.md - Tạo liên kết công khai qua /share-public (allowDownload=true)
    @Test
    @DisplayName("3.4 (extra) - POST /api/v1/documents/{id}/share-public - allowDownload=true - 201 Created")
    void scenario34_CreatePublicLink_AllowDownload_Created() throws Exception {
        Long docId = 305L;
        Instant expiryAt = Instant.parse("2026-01-31T23:59:59Z");
        when(documentService.createShareLink(eq(docId), eq(expiryAt), eq(true)))
                .thenReturn("https://example.com/share/public/" + docId + "?dl=1");

        mockMvc.perform(post("/api/v1/documents/{id}/share-public", docId)
                        .param("expiryAt", expiryAt.toString())
                        .param("allowDownload", "true"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.link").value("https://example.com/share/public/" + docId + "?dl=1"));
    }
}
