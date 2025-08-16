package com.genifast.dms.controller;

import com.genifast.dms.common.constant.ErrorCode;
import com.genifast.dms.common.exception.ApiException;
import com.genifast.dms.common.handler.GlobalExceptionHandler;
import com.genifast.dms.repository.UserRepository;
import com.genifast.dms.service.DocumentService;
import com.genifast.dms.service.FileStorageService;
import com.genifast.dms.service.util.WatermarkService;
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
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class DocumentScenario19Test {

    private MockMvc mockMvc;

    @Mock private DocumentService documentService;
    @Mock private FileStorageService fileStorageService;
    @Mock private WatermarkService watermarkService;
    @Mock private UserRepository userRepository;

    @BeforeEach
    void setup() {
        DocumentController documentController = new DocumentController(documentService, fileStorageService, watermarkService);
        GlobalExceptionHandler handler = new GlobalExceptionHandler(null, userRepository);
        mockMvc = MockMvcBuilders.standaloneSetup(documentController)
                .setControllerAdvice(handler)
                .build();

        // Đăng nhập mặc định, sẽ thay đổi tùy test
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("test@genifast.edu.vn", "password", "documents:read", "documents:version:read"));
    }

    // [19.1] Truy cập tài liệu PRIVATE từ thiết bị bên ngoài -> 403
    @Test
    @DisplayName("[19.1] Văn thư truy cập doc-07 (PRIVATE) từ thiết bị ngoài (403)")
    void scenario191_ReadPrivateFromExternalDevice_Forbidden() throws Exception {
        // user-vt đăng nhập
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("vanthu.tchc@genifast.edu.vn", "password", "documents:read"));

        Long docId = 7L; // map cho doc-07
        Mockito.when(documentService.getDocumentMetadata(docId))
                .thenThrow(new ApiException(ErrorCode.ACCESS_DENIED, "Access denied from external device for private document."));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/documents/{id}", docId)
                        .header("X-Device-Id", "device-003"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message", Matchers.containsString("external device")));
    }

    // [19.2] Truy cập tài liệu đã bị thu hồi -> 403
    @Test
    @DisplayName("[19.2] Hiệu trưởng đọc doc-01 (REVOKED) bị chặn (403)")
    void scenario192_ReadRevokedDocument_Forbidden() throws Exception {
        // user-ht đăng nhập
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("hieutruong@genifast.edu.vn", "password", "documents:read"));

        Long docId = 1L; // doc-01
        Mockito.when(documentService.getDocumentMetadata(docId))
                .thenThrow(new ApiException(ErrorCode.ACCESS_DENIED, "Document has been revoked."));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/documents/{id}", docId)
                        .header("X-Device-Id", "device-001"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message", Matchers.containsString("revoked")));
    }

    // [19.3] Truy cập tài liệu đã bị thay thế (version) -> 403
    @Test
    @DisplayName("[19.3] Chuyên viên đọc version cũ doc-01 đã bị thay thế (403)")
    void scenario193_ReadReplacedVersion_Forbidden() throws Exception {
        // user-cv đăng nhập
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("chuyenvien.dtao@genifast.edu.vn", "password", "documents:version:read"));

        Long docId = 1L; // doc-01
        Integer version = 1; // version cũ
        Mockito.when(documentService.getSpecificDocumentVersion(docId, version))
                .thenThrow(new ApiException(ErrorCode.ACCESS_DENIED, "Document version has been replaced."));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/documents/{id}/versions/{version}", docId, version)
                        .header("X-Device-Id", "device-002"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message", Matchers.containsString("replaced")));
    }
}
