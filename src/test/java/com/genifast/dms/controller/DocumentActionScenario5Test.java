package com.genifast.dms.controller;

// import com.genifast.dms.common.constant.ErrorCode; // removed unused
import com.genifast.dms.dto.request.AuditLogRequest;
import com.genifast.dms.entity.User;
import com.genifast.dms.dto.response.DocumentResponse;
import com.genifast.dms.service.DocumentService;
import com.genifast.dms.service.FileStorageService;
import com.genifast.dms.service.util.WatermarkService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static com.genifast.dms.testutil.TestData.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("DocumentController - Action Scenario 5 (Văn thư)")
class DocumentActionScenario5Test {

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

    // [5.1] theo ai/kich ban.md - Đóng dấu (sign) tài liệu PUBLIC trước khi chia sẻ công khai -> 201 Created
    @Test
    @DisplayName("5.1 - POST /api/v1/documents/{id}/sign - PUBLIC stamp/sign - 201 Created")
    void scenario51_Public_Sign_Created() throws Exception {
        Long docId = DOC_PUBLIC_06; // đại diện doc-06 (PUBLIC, APPROVED)

        doNothing().when(documentService).signDocument(eq(docId));

        mockMvc.perform(post("/api/v1/documents/{id}/sign", docId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Tài liệu đã được ký thành công."));
    }

    // [5.2] theo ai/kich ban.md - Từ chối tài liệu DRAFT do thiếu ký số -> 200 OK
    @Test
    @DisplayName("5.2 - POST /api/v1/documents/{id}/reject?reason=... - INTERNAL DRAFT reject - 200 OK")
    void scenario52_InternalDraft_Reject_Ok() throws Exception {
        Long docId = DOC_INTERNAL_08; // đại diện doc-08 (INTERNAL, DRAFT)

        when(documentService.rejectDocument(eq(docId), eq("Missing digital signature")))
                .thenReturn(new DocumentResponse());

        mockMvc.perform(post("/api/v1/documents/{id}/reject", docId)
                        .param("reason", "Missing digital signature"))
                .andExpect(status().isOk());
    }

    // [5.3] theo ai/kich ban.md - Ký tài liệu INTERNAL thành công, ký PRIVATE bị 403 Forbidden
    @Test
    @DisplayName("5.3 - POST /api/v1/documents/{id}/sign - INTERNAL ok then PRIVATE forbidden")
    void scenario53_Internal_OK_Private_Forbidden() throws Exception {
        // Bước 2: INTERNAL ký thành công
        Long internalId = DOC_INTERNAL_01; // doc-01 (INTERNAL)
        doNothing().when(documentService).signDocument(eq(internalId));
        mockMvc.perform(post("/api/v1/documents/{id}/sign", internalId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Tài liệu đã được ký thành công."));

        // Bước 3: PRIVATE ký bị chặn 403
        Long privateId = DOC_PRIVATE_07; // doc-07 (PRIVATE)
        setAuthenticatedUser("user-vt@example.com", 1501L);
        doThrow(new AccessDeniedException("User not authorized for private document."))
                .when(documentService).signDocument(eq(privateId));

        mockMvc.perform(post("/api/v1/documents/{id}/sign", privateId))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("Bạn không có quyền truy cập hay thực hiện hành động này."))
                .andExpect(jsonPath("$.path").value("/api/v1/documents/" + privateId + "/sign"));

        // verify audit log called
        org.mockito.ArgumentCaptor<AuditLogRequest> logCaptor = org.mockito.ArgumentCaptor.forClass(AuditLogRequest.class);
        org.mockito.Mockito.verify(auditLogService).logAction(logCaptor.capture());
        AuditLogRequest logReq = logCaptor.getValue();
        org.assertj.core.api.Assertions.assertThat(logReq.getAction()).isEqualTo("ACCESS_DENIED");
        org.assertj.core.api.Assertions.assertThat(logReq.getUserId()).isEqualTo(1501L);
    }
}
