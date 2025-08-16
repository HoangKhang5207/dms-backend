package com.genifast.dms.controller;

import com.genifast.dms.dto.request.AuditLogRequest;
import com.genifast.dms.dto.response.DocumentResponse;
import com.genifast.dms.entity.User;
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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static com.genifast.dms.testutil.TestData.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("DocumentController - Review Scenario 6 (Pháp chế)")
class DocumentReviewScenario6Test {

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

    // [6.1] theo ai/kich ban.md - Phê duyệt tài liệu EXTERNAL -> 200 OK
    @Test
    @DisplayName("6.1 - POST /api/v1/documents/{id}/approve - EXTERNAL approve - 200 OK")
    void scenario61_External_Approve_Ok() throws Exception {
        long docExternal05 = DOC_EXTERNAL_05; // đại diện doc-05 (EXTERNAL, APPROVED) theo kế hoạch kiểm thử

        when(documentService.approveDocument(eq(docExternal05)))
                .thenReturn(new DocumentResponse());

        mockMvc.perform(post("/api/v1/documents/{id}/approve", docExternal05)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    // [6.2] theo ai/kich ban.md - Từ chối tài liệu DRAFT do không hợp pháp -> 200 OK
    @Test
    @DisplayName("6.2 - POST /api/v1/documents/{id}/reject?reason=... - INTERNAL DRAFT reject - 200 OK")
    void scenario62_InternalDraft_Reject_Ok() throws Exception {
        Long docDraft08 = DOC_INTERNAL_08; // doc-08 (INTERNAL, DRAFT)

        when(documentService.rejectDocument(eq(docDraft08), eq("Non-compliant with regulations")))
                .thenReturn(new DocumentResponse());

        mockMvc.perform(post("/api/v1/documents/{id}/reject", docDraft08)
                        .param("reason", "Non-compliant with regulations"))
                .andExpect(status().isOk());
    }

    // [6.3] theo ai/kich ban.md - Approve PRIVATE trong quyền (200), sau đó Reject tài liệu không liên quan (403)
    @Test
    @DisplayName("6.3 - POST /{id}/approve ok then /{id}/reject forbidden - PRIVATE approve OK, INTERNAL reject 403")
    void scenario63_Private_Approve_Ok_Internal_Reject_Forbidden() throws Exception {
        // Bước 2: Approve PRIVATE doc-07 thành công
        Long privateDocId = DOC_PRIVATE_07; // doc-07 (PRIVATE, APPROVED)
        when(documentService.approveDocument(eq(privateDocId)))
                .thenReturn(new DocumentResponse());

        mockMvc.perform(post("/api/v1/documents/{id}/approve", privateDocId))
                .andExpect(status().isOk());

        // Bước 3: Reject tài liệu INTERNAL (đại diện doc-02) bị chặn 403
        Long internalDocId = DOC_INTERNAL_01; // đại diện doc-02 (INTERNAL, APPROVED) cho bước ngoài quyền
        setAuthenticatedUser("phapche.bgh@genifast.edu.vn", 1601L);
        doThrow(new AccessDeniedException("User not authorized to review document."))
                .when(documentService).rejectDocument(eq(internalDocId), eq("No business relevance"));

        mockMvc.perform(post("/api/v1/documents/{id}/reject", internalDocId)
                        .param("reason", "No business relevance"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("Bạn không có quyền truy cập hay thực hiện hành động này."))
                .andExpect(jsonPath("$.path").value("/api/v1/documents/" + internalDocId + "/reject"));

        // verify audit log called for ACCESS_DENIED
        org.mockito.ArgumentCaptor<AuditLogRequest> logCaptor = org.mockito.ArgumentCaptor.forClass(AuditLogRequest.class);
        org.mockito.Mockito.verify(auditLogService).logAction(logCaptor.capture());
        AuditLogRequest logReq = logCaptor.getValue();
        org.assertj.core.api.Assertions.assertThat(logReq.getAction()).isEqualTo("ACCESS_DENIED");
        org.assertj.core.api.Assertions.assertThat(logReq.getUserId()).isEqualTo(1601L);
    }
}
