package com.genifast.dms.controller;

import com.genifast.dms.common.constant.ErrorCode;
import com.genifast.dms.common.exception.ApiException;
import com.genifast.dms.common.handler.GlobalExceptionHandler;
import com.genifast.dms.dto.response.DocumentResponse;
import com.genifast.dms.service.DocumentService;
import com.genifast.dms.service.FileStorageService;
import com.genifast.dms.service.util.WatermarkService;
import com.genifast.dms.repository.UserRepository;
import com.genifast.dms.service.AuditLogService;

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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class DocumentScenario12Test {

    private MockMvc mockMvc;

    @Mock
    private DocumentService documentService;
    @Mock
    private FileStorageService fileStorageService;
    @Mock
    private WatermarkService watermarkService;

    @Mock
    private AuditLogService auditLogService;
    @Mock
    private UserRepository userRepository;

    @BeforeEach
    void setup() {
        DocumentController controller = new DocumentController(documentService, fileStorageService, watermarkService);
        GlobalExceptionHandler handler = new GlobalExceptionHandler(auditLogService, userRepository);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(handler)
                .build();

        // Giả lập user-cv đăng nhập
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("chuyenvien.dtao@genifast.edu.vn", "password", "ROLE_USER"));
    }

    private DocumentResponse buildDocResponse(Long id, String title, Integer status, Integer accessType, Long categoryId) {
        DocumentResponse resp = new DocumentResponse();
        resp.setId(id);
        resp.setTitle(title);
        resp.setDescription("");
        resp.setStatus(status);
        resp.setType("DOC");
        resp.setAccessType(accessType);
        resp.setCategoryId(categoryId);
        resp.setDepartmentId(2L);
        resp.setOrganizationId(1L);
        resp.setOriginalFilename("sample.pdf");
        resp.setStorageUnit("local");
        resp.setFilePath("/files/sample.pdf");
        resp.setCreatedBy("chuyenvien.dtao@genifast.edu.vn");
        resp.setCreatedAt(Instant.now());
        resp.setUpdatedAt(Instant.now());
        return resp;
    }

    // [12.1] Tạo (multipart: metadata+file) và trình duyệt tài liệu (submit)
    @Test
    @DisplayName("[12.1] Chuyên viên tạo tài liệu (201) và submit (200)")
    void scenario121_CreateAndSubmitDocument() throws Exception {
        // Arrange: mock create -> 201
        Long newId = 101L;
        DocumentResponse created = buildDocResponse(newId, "Quy chế tuyển sinh 2026", 0, 2, 11L);
        Mockito.when(documentService.createDocument(Mockito.anyString(), Mockito.any()))
                .thenReturn(created);

        // multipart: metadata (String) + file
        String metadataJson = "{" +
                "\"title\":\"Quy chế tuyển sinh 2026\"," +
                "\"status\":\"DRAFT\"," +
                "\"department\":\"P.DTAO\"," +
                "\"recipients\":[\"user-ht\",\"user-cv\"]," +
                "\"confidentiality\":\"INTERNAL\"" +
                "}";
        MockMultipartFile metadataPart = new MockMultipartFile(
                "metadata", "metadata", MediaType.APPLICATION_JSON_VALUE, metadataJson.getBytes());
        MockMultipartFile filePart = new MockMultipartFile(
                "file", "sample.pdf", MediaType.APPLICATION_PDF_VALUE, "PDFDATA".getBytes());

        // Act + Assert: create
        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/documents")
                        .file(metadataPart)
                        .file(filePart))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(newId))
                .andExpect(jsonPath("$.title").value("Quy chế tuyển sinh 2026"));

        // Arrange: mock submit -> 200
        DocumentResponse submitted = buildDocResponse(newId, "Quy chế tuyển sinh 2026", 1, 2, 11L);
        Mockito.when(documentService.submitDocument(newId)).thenReturn(submitted);

        // Act + Assert: submit
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/documents/{id}/submit", newId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(1));
    }

    // [12.2] Phân phối tài liệu ngoài quyền -> 403
    @Test
    @DisplayName("[12.2] Chuyên viên phân phối tài liệu bị chặn 403")
    void scenario122_DistributeForbidden() throws Exception {
        Long docId = 201L;
        List<Long> departmentIds = List.of(3L); // ví dụ phân phối tới P.TCHC

        // Mock service ném ACCESS_DENIED
        Mockito.doThrow(new ApiException(ErrorCode.ACCESS_DENIED, "Bạn không có quyền truy cập hay thực hiện hành động này."))
                .when(documentService).distributeDocument(Mockito.eq(docId), Mockito.eq(departmentIds));

        String payload = "[3]"; // JSON array departmentIds

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/documents/{id}/distribute", docId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message", Matchers.containsString("Bạn không có quyền")));
    }

    // [12.3] Trình duyệt tài liệu PRIVATE trong quyền -> 200
    @Test
    @DisplayName("[12.3] Chuyên viên submit tài liệu PRIVATE (200)")
    void scenario123_SubmitPrivateDocument() throws Exception {
        Long docId = 707L; // giả lập doc-07
        DocumentResponse submitted = buildDocResponse(docId, "Danh sách sinh viên", 1, 2, 99L);
        Mockito.when(documentService.submitDocument(docId)).thenReturn(submitted);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/documents/{id}/submit", docId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(docId))
                .andExpect(jsonPath("$.title").value("Danh sách sinh viên"))
                .andExpect(jsonPath("$.status").value(1));
    }
}
