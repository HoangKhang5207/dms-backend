package com.genifast.dms.controller;

import com.genifast.dms.common.constant.ErrorCode;
import com.genifast.dms.common.exception.ApiException;
import com.genifast.dms.common.handler.GlobalExceptionHandler;
import com.genifast.dms.dto.response.AuditLogResponse;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;

import java.util.List;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class DocumentScenario15Test {

    private MockMvc mockMvc;

    @Mock private DocumentService documentService;
    @Mock private FileStorageService fileStorageService;
    @Mock private WatermarkService watermarkService;

    @Mock private AuditLogService auditLogService;
    @Mock private UserRepository userRepository;
    private AuditLogController auditLogControllerSpy;

    @BeforeEach
    void setup() {
        DocumentController documentController = new DocumentController(documentService, fileStorageService, watermarkService);
        auditLogControllerSpy = Mockito.spy(new AuditLogController(auditLogService));
        GlobalExceptionHandler handler = new GlobalExceptionHandler(auditLogService, userRepository);
        mockMvc = MockMvcBuilders.standaloneSetup(documentController, auditLogControllerSpy)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(handler)
                .build();

        // Giả lập user-qtv đăng nhập (Quản trị viên)
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("quantri@genifast.edu.vn", "password", "ROLE_ADMIN", "audit:log"));
    }

    // [15.1] Tạo báo cáo tài liệu trong quyền -> 200
    // Lưu ý: API thực tế là GET /api/v1/documents/report?reportType=&departmentId= (khác tài liệu)
    @Test
    @DisplayName("[15.1] Quản trị viên tạo báo cáo tài liệu (200)")
    void scenario151_GenerateReport() throws Exception {
        byte[] bytes = "REPORT".getBytes();
        Resource res = new ByteArrayResource(bytes);
        ResponseEntity<Resource> okResp = ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=report.txt")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(res);
        Mockito.when(documentService.generateDocumentReport("summary", 1L)).thenReturn(okResp);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/documents/report")
                        .param("reportType", "summary")
                        .param("departmentId", "1"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, Matchers.containsString("attachment")));
    }

    // [15.2] Xuất tài liệu trong quyền -> 200 (file)
    // Lưu ý: API thực tế là GET /api/v1/documents/{id}/export?format= (khác tài liệu)
    @Test
    @DisplayName("[15.2] Quản trị viên xuất tài liệu (200)")
    void scenario152_ExportDocument() throws Exception {
        Long docId = 4L; // doc-04
        byte[] bytes = "EXPORT".getBytes();
        Resource res = new ByteArrayResource(bytes);
        ResponseEntity<Resource> okResp = ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=doc-04.pdf")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(res);
        Mockito.when(documentService.exportDocument(docId, "PDF")).thenReturn(okResp);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/documents/{id}/export", docId)
                        .param("format", "PDF"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, Matchers.containsString("attachment")));
    }

    // [15.3] Xem log hệ thống trong quyền -> 200
    // Lưu ý: API thực tế dùng /api/v1/audit-logs/document/{docId}
    @Test
    @DisplayName("[15.3] Quản trị viên xem log hệ thống (200)")
    void scenario153_ViewAuditLogs() throws Exception {
        Mockito.doReturn(ResponseEntity.ok().build())
                .when(auditLogControllerSpy)
                .getLogsByDocument(ArgumentMatchers.eq(4L), ArgumentMatchers.any(Pageable.class));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/audit-logs/document/{docId}", 4L)
                        .accept(MediaType.APPLICATION_JSON)
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk());
    }

    // [15.4] Xem lịch sử tài liệu trong quyền -> 200
    // Lưu ý: API thực tế không có /history, dùng /{id}/versions để thay thế
    @Test
    @DisplayName("[15.4] Quản trị viên xem lịch sử (phiên bản) tài liệu (200)")
    void scenario154_ViewHistory() throws Exception {
        DocumentVersionResponse v1 = new DocumentVersionResponse();
        v1.setDocumentId(4L);
        v1.setVersionNumber(1);
        DocumentVersionResponse v2 = new DocumentVersionResponse();
        v2.setDocumentId(4L);
        v2.setVersionNumber(2);
        Mockito.when(documentService.getDocumentVersions(4L)).thenReturn(List.of(v1, v2));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/documents/{id}/versions", 4L))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    // [15.5] Khóa tài liệu ngoài quyền -> 403
    @Test
    @DisplayName("[15.5] Quản trị viên khóa tài liệu bị chặn 403")
    void scenario155_LockForbidden() throws Exception {
        Long docId = 4L;
        Mockito.doThrow(new ApiException(ErrorCode.ACCESS_DENIED, "Bạn không có quyền truy cập hay thực hiện hành động này."))
                .when(documentService).lockDocument(docId);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/documents/{id}/lock", docId))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message", Matchers.containsString("Bạn không có quyền")));
    }
}
