package com.genifast.dms.controller;

import com.genifast.dms.dto.request.AuditLogRequest;
import com.genifast.dms.entity.Document;
import com.genifast.dms.repository.DocumentRepository;
import com.genifast.dms.service.AuditLogService;
import com.genifast.dms.service.FileStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("PublicDocumentController - Scenario 7 (Visitor)")
class PublicDocumentScenario7Test {

    private MockMvc mockMvc;

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private com.genifast.dms.repository.UserRepository userRepository;

    @BeforeEach
    void setup() {
        PublicDocumentController controller = new PublicDocumentController(documentRepository, fileStorageService, auditLogService);
        com.genifast.dms.common.handler.GlobalExceptionHandler advice =
                new com.genifast.dms.common.handler.GlobalExceptionHandler(auditLogService, userRepository);
        this.mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(advice)
                .build();
    }

    private Document buildPublicDoc(Long id, String token, boolean allowDownload) {
        return Document.builder()
                .id(id)
                .title("Public Doc")
                .type("pdf")
                .originalFilename("public-doc.pdf")
                .shareToken(token)
                .publicShareExpiryAt(Instant.now().plusSeconds(3600))
                .allowPublicDownload(allowDownload)
                .build();
    }

    // [7.1] Preview PUBLIC (partial, watermark) -> 200 OK
    @Test
    @DisplayName("7.1 - GET /api/v1/documents/public/{token}/preview - 200 OK")
    void scenario71_PreviewPublic_Ok() throws Exception {
        String token = "token-preview-123";
        Document doc = buildPublicDoc(701L, token, false);
        when(documentRepository.findByShareToken(eq(token))).thenReturn(Optional.of(doc));
        when(fileStorageService.retrieveFileForVisitor(eq(doc))).thenReturn("preview".getBytes());

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/documents/public/{token}/preview", token))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("inline")))
                .andExpect(content().contentType(MediaType.parseMediaType("application/pdf")))
                .andExpect(content().bytes("preview".getBytes()));

        ArgumentCaptor<AuditLogRequest> captor = ArgumentCaptor.forClass(AuditLogRequest.class);
        verify(auditLogService).logAction(captor.capture());
        assertThat(captor.getValue().getAction()).isEqualTo("PREVIEW_DOCUMENT_PUBLIC");
        assertThat(captor.getValue().getDocumentId()).isEqualTo(701L);
    }

    // [7.2] View full PUBLIC sau trả phí -> 200 OK (header X-Payment-Status=paid)
    @Test
    @DisplayName("7.2 - GET /api/v1/documents/public/{token} - paid - 200 OK")
    void scenario72_ViewFull_Paid_Ok() throws Exception {
        String token = "token-view-456";
        Document doc = buildPublicDoc(702L, token, false);
        when(documentRepository.findByShareToken(eq(token))).thenReturn(Optional.of(doc));
        when(fileStorageService.retrieveFileForVisitor(eq(doc))).thenReturn("full".getBytes());

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/documents/public/{token}", token)
                        .header("X-Payment-Status", "paid"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("inline")))
                .andExpect(content().contentType(MediaType.parseMediaType("application/pdf")))
                .andExpect(content().bytes("full".getBytes()));

        ArgumentCaptor<AuditLogRequest> captor = ArgumentCaptor.forClass(AuditLogRequest.class);
        verify(auditLogService).logAction(captor.capture());
        assertThat(captor.getValue().getAction()).isEqualTo("READ_DOCUMENT_PUBLIC");
        assertThat(captor.getValue().getDocumentId()).isEqualTo(702L);
    }

    // [7.4] Truy cập full khi chưa trả phí -> 403 Forbidden
    @Test
    @DisplayName("7.4 - GET /api/v1/documents/public/{token} - unpaid - 403")
    void scenario74_ViewFull_Unpaid_Forbidden() throws Exception {
        String token = "token-view-789";
        Document doc = buildPublicDoc(704L, token, false);
        when(documentRepository.findByShareToken(eq(token))).thenReturn(Optional.of(doc));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/documents/public/{token}", token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403001))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("Payment required for full access."))
                .andExpect(jsonPath("$.path").value("/api/v1/documents/public/" + token));

        ArgumentCaptor<AuditLogRequest> captor = ArgumentCaptor.forClass(AuditLogRequest.class);
        verify(auditLogService).logAction(captor.capture());
        assertThat(captor.getValue().getAction()).isEqualTo("READ_DOCUMENT_PUBLIC_FAILED");
        assertThat(captor.getValue().getDocumentId()).isEqualTo(704L);
    }

    // [7.3] Download PUBLIC sau trả phí -> 200 OK (header X-Payment-Status=paid_download)
    @Test
    @DisplayName("7.3 - GET /api/v1/documents/public/{token}/download - paid_download - 200 OK")
    void scenario73_Download_Paid_Ok() throws Exception {
        String token = "token-dl-abc";
        Document doc = buildPublicDoc(703L, token, true);
        when(documentRepository.findByShareToken(eq(token))).thenReturn(Optional.of(doc));
        when(fileStorageService.retrieveFileForVisitor(eq(doc))).thenReturn("filedata".getBytes());

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/documents/public/{token}/download", token)
                        .header("X-Payment-Status", "paid_download"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("attachment")))
                .andExpect(content().contentType(MediaType.parseMediaType("application/pdf")))
                .andExpect(content().bytes("filedata".getBytes()));

        ArgumentCaptor<AuditLogRequest> captor = ArgumentCaptor.forClass(AuditLogRequest.class);
        verify(auditLogService).logAction(captor.capture());
        assertThat(captor.getValue().getAction()).isEqualTo("DOWNLOAD_DOCUMENT_PUBLIC");
        assertThat(captor.getValue().getDocumentId()).isEqualTo(703L);
    }

    // [7.5] Token không hợp lệ -> 403 Forbidden
    @Test
    @DisplayName("7.5 - GET /api/v1/documents/public/{token}/preview - invalid token - 403")
    void scenario75_InvalidToken_Forbidden() throws Exception {
        String token = "invalid-token-xyz";
        when(documentRepository.findByShareToken(eq(token))).thenReturn(Optional.empty());

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/documents/public/{token}/preview", token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403001))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("Invalid or unauthorized public link."))
                .andExpect(jsonPath("$.path").value("/api/v1/documents/public/" + token + "/preview"));

        ArgumentCaptor<AuditLogRequest> captor = ArgumentCaptor.forClass(AuditLogRequest.class);
        verify(auditLogService).logAction(captor.capture());
        assertThat(captor.getValue().getAction()).isEqualTo("READ_DOCUMENT_PUBLIC_FAILED");
        assertThat(captor.getValue().getDocumentId()).isNull();
    }
}
