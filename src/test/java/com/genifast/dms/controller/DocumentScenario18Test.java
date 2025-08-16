package com.genifast.dms.controller;

import com.genifast.dms.common.constant.ErrorCode;
import com.genifast.dms.common.exception.ApiException;
import com.genifast.dms.common.handler.GlobalExceptionHandler;
import com.genifast.dms.dto.request.DocumentUpdateRequest;
import com.genifast.dms.repository.UserRepository;
import com.genifast.dms.service.DocumentService;
import com.genifast.dms.service.FileStorageService;
import com.genifast.dms.service.util.WatermarkService;
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
import org.springframework.http.MediaType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class DocumentScenario18Test {

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
    }

    // [18.1] Thao tác ngoài thời gian dự án (Thất bại) -> 403
    @Test
    @DisplayName("[18.1] user-cv cập nhật tài liệu khi dự án đã hết hạn (403)")
    void scenario181_UpdateWhenProjectExpired_Forbidden() throws Exception {
        // Giả lập đăng nhập user-cv (Tổ phó)
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("chuyenvien.dtao@genifast.edu.vn", "password", "ROLE_PROJECT_DEPUTY"));

        Long docId = 1001L; // map cho doc-proj-01
        Mockito.when(documentService.updateDocumentMetadata(ArgumentMatchers.eq(docId), ArgumentMatchers.any(DocumentUpdateRequest.class)))
                .thenThrow(new ApiException(ErrorCode.ACCESS_DENIED, "Project is not active or has expired"));

        String body = "{" +
                "\"title\":\"Cập nhật ngoài thời gian dự án\"" +
                "}";

        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/documents/{id}", docId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message", Matchers.containsString("expired")));
    }

    // [18.2] Thao tác bởi người không phải thành viên (Thất bại) -> 403
    @Test
    @DisplayName("[18.2] user-gv đọc tài liệu khi không phải thành viên dự án (403)")
    void scenario182_ReadByNonMember_Forbidden() throws Exception {
        // Giả lập đăng nhập user-gv (không phải thành viên)
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("giaovu.cntt@genifast.edu.vn", "password", "ROLE_STAFF"));

        Long docId = 1001L; // map cho doc-proj-01
        Mockito.when(documentService.getDocumentMetadata(docId))
                .thenThrow(new ApiException(ErrorCode.ACCESS_DENIED, "User is not a member of the project"));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/documents/{id}", docId))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message", Matchers.containsString("not a member")));
    }
}
