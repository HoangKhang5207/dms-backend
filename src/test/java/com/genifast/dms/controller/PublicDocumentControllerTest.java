package com.genifast.dms.controller;

import com.genifast.dms.entity.Document;
import com.genifast.dms.repository.DocumentRepository;
import com.genifast.dms.service.FileStorageService;
import com.genifast.dms.service.AuditLogService;
import com.genifast.dms.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("PublicDocumentController - Visitor/Share Link Tests")
class PublicDocumentControllerTest {

    private MockMvc mockMvc;

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private FileStorageService fileStorageService;

    // For GlobalExceptionHandler dependencies
    @Mock
    private AuditLogService auditLogService;

    @Mock
    private UserRepository userRepository;

    private Document baseDoc;

    @BeforeEach
    void setup() throws Exception {
        // Rebuild MockMvc with explicit controller + advice so exceptions are handled
        PublicDocumentController controller = new PublicDocumentController(documentRepository, fileStorageService);
        com.genifast.dms.common.handler.GlobalExceptionHandler advice =
                new com.genifast.dms.common.handler.GlobalExceptionHandler(auditLogService, userRepository);
        this.mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(advice)
                .build();
        // Base valid document for token-ok
        baseDoc = new Document();
        baseDoc.setTitle("Tài liệu công khai");
        baseDoc.setContent("Preview only");
        baseDoc.setType("pdf");
        baseDoc.setOriginalFilename("public.pdf");
        baseDoc.setStatus(1);
        baseDoc.setShareToken("token-ok");
        baseDoc.setPublicShareExpiryAt(Instant.now().plusSeconds(3600));
        baseDoc.setAllowPublicDownload(true);
        baseDoc.setFileId("file-123");

        // Expired token document
        Document expired = new Document();
        expired.setTitle("Expired");
        expired.setContent("Preview only");
        expired.setType("pdf");
        expired.setOriginalFilename("expired.pdf");
        expired.setStatus(1);
        expired.setShareToken("token-exp");
        expired.setPublicShareExpiryAt(Instant.now().minusSeconds(5));
        expired.setAllowPublicDownload(true);
        expired.setFileId("file-exp");

        // No-download document
        Document noDl = new Document();
        noDl.setTitle("NoDL");
        noDl.setContent("Preview only");
        noDl.setType("pdf");
        noDl.setOriginalFilename("nodl.pdf");
        noDl.setStatus(1);
        noDl.setShareToken("token-nodl");
        noDl.setPublicShareExpiryAt(Instant.now().plusSeconds(3600));
        noDl.setAllowPublicDownload(false);
        noDl.setFileId("file-nodl");

        // Stub repository by token
        when(documentRepository.findByShareToken("token-ok")).thenReturn(java.util.Optional.of(baseDoc));
        when(documentRepository.findByShareToken("token-exp")).thenReturn(java.util.Optional.of(expired));
        when(documentRepository.findByShareToken("token-nodl")).thenReturn(java.util.Optional.of(noDl));

        // mock file bytes returned for visitor
        when(fileStorageService.retrieveFileForVisitor(any(Document.class))).thenReturn("dummy".getBytes());
    }

    @Test
    @DisplayName("Public share: token hết hạn -> 400 Bad Request")
    void getSharedDocument_ExpiredToken_BadRequest() throws Exception {
        mockMvc.perform(get("/api/public/documents/share/{token}", "token-exp")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Public share: không cho tải xuống -> 400 Bad Request (USER_NO_PERMISSION)")
    void getSharedDocument_NotAllowDownload_Forbidden() throws Exception {
        mockMvc.perform(get("/api/public/documents/share/{token}", "token-nodl")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Public share: cho tải xuống + hợp lệ -> 200 OK")
    void getSharedDocument_AllowDownload_Ok() throws Exception {
        mockMvc.perform(get("/api/public/documents/share/{token}", "token-ok")
                        .accept(MediaType.APPLICATION_OCTET_STREAM))
                .andExpect(status().isOk());
    }
}
