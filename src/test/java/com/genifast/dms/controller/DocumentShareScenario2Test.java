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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static com.genifast.dms.testutil.TestData.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("DocumentController - Share Scenario 2 (Trưởng khoa)")
class DocumentShareScenario2Test {

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

    // [2.1] theo ai/kich ban.md - Tình huống 2.1: Chia sẻ forwardable + timebound -> 201 Created
    @Test
    @DisplayName("2.1 - POST /api/v1/documents/{id}/share - forwardable + timebound - 201 Created")
    void scenario21_ForwardableTimebound_Created() throws Exception {
        Long docId = DOC_SC2_FWD_TIMEBOUND;
        String body = TestData.buildShareBody(
                USER_SC2,
                List.of(PERM_FORWARDABLE, PERM_TIMEBOUND),
                Instant.parse("2025-08-10T00:00:00Z"),
                Instant.parse("2025-09-10T00:00:00Z"),
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
        assertThat(sent.getRecipientId()).isEqualTo(USER_SC2);
        assertThat(sent.getPermissions()).contains(PERM_FORWARDABLE, PERM_TIMEBOUND);
        assertThat(sent.getFromDate()).isNotNull();
        assertThat(sent.getToDate()).isNotNull();
    }

    // [2.2] theo ai/kich ban.md - Tình huống 2.2: Chia sẻ ra ngoài tổ chức không có quyền external -> 403 Forbidden
    @Test
    @DisplayName("2.2 - POST /api/v1/documents/{id}/share - out of organization - 403 Forbidden")
    void scenario22_ShareOutOfOrganization_Forbidden() throws Exception {
        Long docId = DOC_SC2_EXTERNAL_FORBIDDEN; // đại diện doc-02
        String body = TestData.buildShareBody(
                USER_EXTERNAL,
                List.of(PERM_READONLY),
                null,
                null,
                true
        );

        setAuthenticatedUser("user-tk@example.com", 1201L);
        doThrow(new AccessDeniedException("Recipient is not in the same organization."))
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
        org.assertj.core.api.Assertions.assertThat(logReq.getUserId()).isEqualTo(1201L);
    }

    // [2.3] theo ai/kich ban.md - Tình huống 2.3: Chia sẻ tài liệu PUBLIC với quyền shareable -> 201 Created
    @Test
    @DisplayName("2.3 - POST /api/v1/documents/{id}/share - PUBLIC + shareable - 201 Created")
    void scenario23_PublicShareable_Created() throws Exception {
        Long docId = DOC_SC2_PUBLIC_SHAREABLE; // đại diện doc-03 (PUBLIC)
        String body = TestData.buildShareBody(
                2003L,
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
        assertThat(sent.getRecipientId()).isEqualTo(2003L);
        assertThat(sent.getPermissions()).containsExactly(PERM_SHAREABLE);
    }
}
