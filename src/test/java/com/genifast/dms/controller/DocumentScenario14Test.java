package com.genifast.dms.controller;

import com.genifast.dms.common.constant.ErrorCode;
import com.genifast.dms.common.exception.ApiException;
import com.genifast.dms.common.handler.GlobalExceptionHandler;
import com.genifast.dms.dto.response.DocumentVersionResponse;
import com.genifast.dms.service.AuditLogService;
import com.genifast.dms.service.DocumentService;
import com.genifast.dms.service.FileStorageService;
import com.genifast.dms.service.util.WatermarkService;
import com.genifast.dms.repository.UserRepository;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class DocumentScenario14Test {

    private MockMvc mockMvc;

    @Mock private DocumentService documentService;
    @Mock private FileStorageService fileStorageService;
    @Mock private WatermarkService watermarkService;

    @Mock private AuditLogService auditLogService;
    @Mock private UserRepository userRepository;

    @BeforeEach
    void setup() {
        DocumentController controller = new DocumentController(documentService, fileStorageService, watermarkService);
        GlobalExceptionHandler handler = new GlobalExceptionHandler(auditLogService, userRepository);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(handler)
                .build();

        // Giả lập user-lt đăng nhập (Nhân viên Lưu trữ)
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("luutru@genifast.edu.vn", "password", "ROLE_USER"));
    }

    // [14.1] Lưu trữ tài liệu trong quyền -> 201
    @Test
    @DisplayName("[14.1] Lưu trữ tài liệu (201)")
    void scenario141_Archive() throws Exception {
        Long docId = 6L; // doc-06
        Mockito.doNothing().when(documentService).archiveDocument(docId);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/documents/{id}/archive", docId))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").exists());
    }

    // [14.2] Khôi phục tài liệu trong quyền -> 201
    @Test
    @DisplayName("[14.2] Khôi phục tài liệu (201)")
    void scenario142_Restore() throws Exception {
        Long docId = 6L; // doc-06
        Mockito.doNothing().when(documentService).restoreDocument(docId);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/documents/{id}/restore", docId))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").exists());
    }

    // [14.3] Tải xuống tài liệu trong quyền -> 200
    @Test
    @DisplayName("[14.3] Tải xuống tài liệu (200)")
    void scenario143_Download() throws Exception {
        Long docId = 6L; // doc-06
        byte[] bytes = "FILEDATA".getBytes();
        Resource res = new ByteArrayResource(bytes);
        ResponseEntity<Resource> okResp = ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=doc-06.pdf")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(res);
        Mockito.when(documentService.downloadDocumentFile(docId)).thenReturn(okResp);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/documents/{id}/download", docId))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, Matchers.containsString("attachment")));
    }

    // [14.4] Xem phiên bản tài liệu trong quyền -> 200
    @Test
    @DisplayName("[14.4] Xem phiên bản tài liệu (200)")
    void scenario144_ViewVersion() throws Exception {
        Long docId = 4L; // doc-04
        Integer versionNumber = 1;
        Mockito.when(documentService.getSpecificDocumentVersion(docId, versionNumber))
                .thenReturn(new DocumentVersionResponse());

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/documents/{id}/versions/{version}", docId, versionNumber))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    // [14.5] Phê duyệt tài liệu ngoài quyền -> 403
    @Test
    @DisplayName("[14.5] Phê duyệt tài liệu bị chặn 403")
    void scenario145_ApproveForbidden() throws Exception {
        Long docId = 6L; // doc-06
        Mockito.when(documentService.approveDocument(ArgumentMatchers.eq(docId)))
                .thenThrow(new ApiException(ErrorCode.ACCESS_DENIED, "Bạn không có quyền truy cập hay thực hiện hành động này."));

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/documents/{id}/approve", docId))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message", Matchers.containsString("Bạn không có quyền")));
    }
}
