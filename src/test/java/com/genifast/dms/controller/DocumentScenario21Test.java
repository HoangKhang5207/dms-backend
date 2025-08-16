package com.genifast.dms.controller;

import com.genifast.dms.common.constant.ErrorCode;
import com.genifast.dms.common.exception.ApiException;
import com.genifast.dms.common.handler.GlobalExceptionHandler;
import com.genifast.dms.dto.response.DocumentVersionResponse;
import com.genifast.dms.entity.Document;
import com.genifast.dms.repository.DocumentRepository;
import com.genifast.dms.repository.UserRepository;
import com.genifast.dms.service.AuditLogService;
import com.genifast.dms.service.DocumentService;
import com.genifast.dms.service.FileStorageService;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.Optional;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class DocumentScenario21Test {

    private MockMvc mockMvc;

    @Mock private DocumentService documentService;
    @Mock private FileStorageService fileStorageService;
    @Mock private DocumentRepository documentRepository;
    @Mock private AuditLogService auditLogService;
    @Mock private UserRepository userRepository;

    private static Document buildPublicDoc06() {
        Document d = new Document();
        d.setId(6L);
        d.setShareToken("token-abc123");
        d.setPublicShareExpiryAt(Instant.now().plusSeconds(3600));
        d.setAllowPublicDownload(true);
        d.setOriginalFilename("ke-hoach-dao-tao-2025.pdf");
        d.setType("pdf");
        return d;
    }

    @BeforeEach
    void setup() {
        DocumentController internalController = new DocumentController(documentService, fileStorageService, null);
        PublicDocumentController publicController = new PublicDocumentController(documentRepository, fileStorageService, auditLogService);
        GlobalExceptionHandler handler = new GlobalExceptionHandler(null, userRepository);
        mockMvc = MockMvcBuilders.standaloneSetup(internalController, publicController)
                .setControllerAdvice(handler)
                .build();
    }

    // [21.1] Trưởng khoa xem phiên bản cũ của tài liệu -> 200
    @Test
    @DisplayName("[21.1] Trưởng khoa xem phiên bản 2 của doc-02 (200)")
    void scenario211_Dean_View_OldVersion_Success() throws Exception {
        Long docId = 2L; // map cho doc-02
        Integer version = 2;
        DocumentVersionResponse resp = new DocumentVersionResponse();
        resp.setDocumentId(docId);
        resp.setVersionNumber(version);
        resp.setTitle("Kế hoạch đào tạo ngành CNTT - v2");

        Mockito.when(documentService.getSpecificDocumentVersion(docId, version)).thenReturn(resp);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/documents/{id}/versions/{ver}", docId, version)
                        .header("X-Device-Id", "device-001"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.documentId").value(docId))
                .andExpect(jsonPath("$.versionNumber").value(version))
                .andExpect(jsonPath("$.title").value("Kế hoạch đào tạo ngành CNTT - v2"));
    }

    // [21.2] Phó khoa xem phiên bản mới nhất của tài liệu -> 200
    @Test
    @DisplayName("[21.2] Phó khoa xem phiên bản 3 của doc-03 (200)")
    void scenario212_ViceDean_View_LatestVersion_Success() throws Exception {
        Long docId = 3L; // map cho doc-03
        Integer version = 3;
        DocumentVersionResponse resp = new DocumentVersionResponse();
        resp.setDocumentId(docId);
        resp.setVersionNumber(version);
        resp.setTitle("Kế hoạch hợp tác quốc tế 2025 - v3");

        Mockito.when(documentService.getSpecificDocumentVersion(docId, version)).thenReturn(resp);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/documents/{id}/versions/{ver}", docId, version)
                        .header("X-Device-Id", "device-001"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.documentId").value(docId))
                .andExpect(jsonPath("$.versionNumber").value(version))
                .andExpect(jsonPath("$.title").value("Kế hoạch hợp tác quốc tế 2025 - v3"));
    }

    // [21.3] Văn thư xem phiên bản PRIVATE không được cấp quyền -> 403
    @Test
    @DisplayName("[21.3] Văn thư xem phiên bản 2 của doc-07 PRIVATE: bị chặn (403)")
    void scenario213_Clerk_View_PrivateVersion_Forbidden() throws Exception {
        Long docId = 7L; // doc-07
        Integer version = 2;
        Mockito.when(documentService.getSpecificDocumentVersion(docId, version))
                .thenThrow(new ApiException(ErrorCode.ACCESS_DENIED, "User not authorized for private document."));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/documents/{id}/versions/{ver}", docId, version)
                        .header("X-Device-Id", "device-004"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("User not authorized for private document."));
    }

    // [21.4] Visitor xem phiên bản 2 của tài liệu PUBLIC sau khi thanh toán -> 200
    @Test
    @DisplayName("[21.4] Visitor mua thành công: xem phiên bản 2 của doc-06 (200)")
    void scenario214_Visitor_View_Version_AfterPurchase_Success() throws Exception {
        String token = "token-abc123";
        Integer version = 2;
        Document doc = buildPublicDoc06();

        Mockito.when(documentRepository.findByShareToken(token)).thenReturn(Optional.of(doc));
        byte[] content = "PDF_BYTES_VERSION2".getBytes();
        Mockito.when(fileStorageService.retrieveFileForVisitor(doc)).thenReturn(content);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/documents/public/{token}/versions/{version}", token, version)
                        .header("X-Payment-Status", "purchased"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", Matchers.containsString("inline")))
                .andExpect(content().contentType(MediaType.parseMediaType("application/pdf")))
                .andExpect(content().bytes(content));
    }
}
