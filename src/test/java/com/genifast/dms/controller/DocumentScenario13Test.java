package com.genifast.dms.controller;

import com.genifast.dms.common.constant.ErrorCode;
import com.genifast.dms.common.exception.ApiException;
import com.genifast.dms.common.handler.GlobalExceptionHandler;
import com.genifast.dms.dto.request.DocumentCommentRequest;
import com.genifast.dms.dto.response.DocumentResponse;
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

import java.time.Instant;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class DocumentScenario13Test {

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

        // Giả lập user-gv đăng nhập (Giáo vụ)
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("giaovu.cntt@genifast.edu.vn", "password", "ROLE_USER"));
    }

    // [13.1] Thêm nhận xét vào tài liệu trong quyền -> 201
    @Test
    @DisplayName("[13.1] Giáo vụ thêm nhận xét vào tài liệu (201)")
    void scenario131_AddComment() throws Exception {
        Long docId = 2L; // doc-02
        // Mock service addComment không trả về, controller trả 201 với chuỗi
        Mockito.doNothing().when(documentService)
                .addComment(ArgumentMatchers.eq(docId), ArgumentMatchers.any(DocumentCommentRequest.class));

        String payload = "{\n  \"content\": \"Cần bổ sung chi tiết môn học\"\n}";

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/documents/{id}/comments", docId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN));
    }

    // [13.2] Từ chối tài liệu ngoài quyền -> 403
    @Test
    @DisplayName("[13.2] Giáo vụ từ chối tài liệu bị chặn 403")
    void scenario132_RejectForbidden() throws Exception {
        Long docId = 2L; // doc-02
        // Mock service ném ACCESS_DENIED -> Handler trả JSON 403 tiếng Việt
        Mockito.when(documentService.rejectDocument(ArgumentMatchers.eq(docId), ArgumentMatchers.anyString()))
                .thenThrow(new ApiException(ErrorCode.ACCESS_DENIED, "Bạn không có quyền truy cập hay thực hiện hành động này."));

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/documents/{id}/reject", docId)
                        .param("reason", "Không phù hợp nội dung"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message", Matchers.containsString("Bạn không có quyền")));
    }

    // [13.3] Tải xuống tài liệu trong quyền -> 200
    @Test
    @DisplayName("[13.3] Giáo vụ tải xuống tài liệu (200)")
    void scenario133_DownloadDocument() throws Exception {
        Long docId = 2L; // doc-02
        byte[] bytes = "FILEDATA".getBytes();
        Resource res = new ByteArrayResource(bytes);
        ResponseEntity<Resource> okResp = ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=plan.pdf")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(res);
        Mockito.when(documentService.downloadDocumentFile(docId)).thenReturn(okResp);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/documents/{id}/download", docId))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, Matchers.containsString("attachment")));
    }
}
