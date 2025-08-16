package com.genifast.dms.controller;

import com.genifast.dms.common.constant.ErrorCode;
import com.genifast.dms.common.exception.ApiException;
import com.genifast.dms.common.handler.GlobalExceptionHandler;
import com.genifast.dms.dto.response.DocumentResponse;
import com.genifast.dms.repository.UserRepository;
import com.genifast.dms.service.DocumentService;
import com.genifast.dms.service.FileStorageService;
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

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class DocumentScenario22Test {

    private MockMvc mockMvc;

    @Mock private DocumentService documentService;
    @Mock private FileStorageService fileStorageService;
    @Mock private UserRepository userRepository;

    @BeforeEach
    void setup() {
        DocumentController controller = new DocumentController(documentService, fileStorageService, null);
        GlobalExceptionHandler handler = new GlobalExceptionHandler(null, userRepository);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(handler)
                .build();
    }

    // [22.1] Chuyên viên truy cập doc-06 đã hết hiệu lực -> 403
    @Test
    @DisplayName("[22.1] Doc EXPIRED: truy cập bị chặn (403)")
    void scenario221_ReadExpiredDocument_Forbidden() throws Exception {
        Long docId = 6L; // doc-06

        Mockito.when(documentService.getDocumentMetadata(docId))
                .thenThrow(new ApiException(ErrorCode.ACCESS_DENIED, "Document has expired."));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/documents/{id}", docId)
                        .header("X-Device-Id", "device-002"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("Document has expired."));
    }
}
