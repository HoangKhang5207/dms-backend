package com.genifast.dms.controller;

import com.genifast.dms.common.handler.GlobalExceptionHandler;
import com.genifast.dms.dto.response.DocumentResponse;
import com.genifast.dms.entity.Department;
import com.genifast.dms.entity.Document;
import com.genifast.dms.entity.Organization;
import com.genifast.dms.entity.User;
import com.genifast.dms.mapper.DocumentMapper;
import com.genifast.dms.repository.*;
import com.genifast.dms.service.AuditLogService;
import com.genifast.dms.service.DocumentService;
import com.genifast.dms.service.FileStorageService;
import com.genifast.dms.service.impl.DocumentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.mockito.Mockito;
import org.hamcrest.Matchers;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("DocumentController - Scenario 11 (RBAC/ABAC Phó khoa)")
class DocumentScenario11Test {

    private MockMvc mockMvc;

    @Mock private DocumentRepository documentRepository;
    @Mock private UserRepository userRepository;
    @Mock private DepartmentRepository departmentRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private PrivateDocumentRepository privateDocumentRepository;
    @Mock private FileStorageService fileStorageService;
    @Mock private DocumentMapper documentMapper;
    @Mock private AuditLogService auditLogService;

    private DocumentService documentService;

    private Organization org;
    private Department deptCNTT;
    private Department deptTCHC;

    @BeforeEach
    void setup() {
        documentService = new DocumentServiceImpl(
                documentRepository,
                userRepository,
                departmentRepository,
                categoryRepository,
                privateDocumentRepository,
                fileStorageService,
                documentMapper,
                new com.fasterxml.jackson.databind.ObjectMapper()
        );
        DocumentController controller = new DocumentController(documentService, fileStorageService, new com.genifast.dms.service.util.WatermarkService());
        GlobalExceptionHandler advice = new GlobalExceptionHandler(auditLogService, userRepository);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(advice)
                .build();

        // Common org/dept fixtures
        org = new Organization();
        org.setId(1L);
        org.setName("GENIFAST EDU");

        deptCNTT = new Department();
        deptCNTT.setId(10L);
        deptCNTT.setName("K.CNTT");
        deptCNTT.setOrganization(org);

        deptTCHC = new Department();
        deptTCHC.setId(20L);
        deptTCHC.setName("P.TCHC");
        deptTCHC.setOrganization(org);

        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(String email, boolean isOrgManager, boolean isDeptManager, Department dept) {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(email, "pwd"));
        when(userRepository.findByEmail(eq(email))).thenReturn(Optional.of(buildUser(email, isOrgManager, isDeptManager, dept)));
    }

    private User buildUser(String email, boolean isOrgManager, boolean isDeptManager, Department dept) {
        User u = new User();
        u.setId(Math.abs(email.hashCode()) + 0L);
        u.setEmail(email);
        u.setIsOrganizationManager(isOrgManager);
        u.setIsDeptManager(isDeptManager);
        u.setDepartment(dept);
        u.setOrganization(org);
        u.setStatus(1);
        return u;
    }

    private Document buildDoc(Long id, int accessType, Department dept, int status, int confidentiality, String createdBy) {
        Document d = Document.builder()
                .id(id)
                .title("Doc-" + id)
                .accessType(accessType)
                .department(dept)
                .organization(org)
                .status(status)
                .confidentiality(confidentiality)
                .originalFilename("doc" + id + ".pdf")
                .type("pdf")
                .filePath("/files/doc" + id + ".pdf")
                .build();
        d.setContentType("application/pdf");
        d.setCreatedBy(createdBy);
        return d;
    }

    private void stubDocumentResponse(Document doc) {
        DocumentResponse resp = new DocumentResponse();
        resp.setId(doc.getId());
        resp.setTitle(doc.getTitle());
        resp.setAccessType(doc.getAccessType());
        resp.setDepartmentId(doc.getDepartment().getId());
        resp.setOrganizationId(doc.getOrganization().getId());
        when(documentMapper.toDocumentResponse(eq(doc))).thenReturn(resp);
    }

    // [11.1] Phó khoa cập nhật metadata tài liệu trong quyền (creator) -> 200 OK
    @Test
    @DisplayName("11.1 - PUT /api/v1/documents/{id} - vice head as creator - 200 OK")
    void scenario111_UpdateDocument_Ok_WhenCreator() throws Exception {
        String email = "pho.khoa.cntt@genifast.edu.vn";
        // Phó khoa KHÔNG là org manager, KHÔNG là dept manager, nhưng là người tạo tài liệu
        authenticateAs(email, false, false, deptCNTT);

        Long docId = 110101L;
        Document doc = buildDoc(docId, 3, deptCNTT, 2, 2, email); // accessType=DEPT, confidentiality=2
        when(documentRepository.findById(eq(docId))).thenReturn(Optional.of(doc));
        when(documentRepository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));
        when(documentMapper.toDocumentResponse(any(Document.class))).thenAnswer(inv -> {
            Document d = inv.getArgument(0);
            DocumentResponse resp = new DocumentResponse();
            resp.setId(d.getId());
            resp.setTitle(d.getTitle());
            resp.setAccessType(d.getAccessType());
            resp.setDepartmentId(d.getDepartment().getId());
            resp.setOrganizationId(d.getOrganization().getId());
            return resp;
        });

        String payload = "{\n" +
                "  \"title\": \"Doc updated\",\n" +
                "  \"description\": \"Mo ta moi\"\n" +
                "}";

        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/documents/{id}", docId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(docId))
                .andExpect(jsonPath("$.title").value("Doc updated"));
    }

    // [11.2] Phó khoa phê duyệt tài liệu nhưng KHÔNG có quyền -> 403 Forbidden
    @Test
    @DisplayName("11.2 - POST /api/v1/documents/{id}/approve - vice head without permission - 403 Forbidden")
    void scenario112_Approve_Forbidden_WhenNoPermission() throws Exception {
        String email = "pho.khoa.cntt@genifast.edu.vn";
        authenticateAs(email, false, false, deptCNTT);

        // Dựng controller riêng dùng DocumentService mock để ném ApiException ACCESS_DENIED
        DocumentService mockedService = Mockito.mock(DocumentService.class);
        DocumentController controller = new DocumentController(mockedService, fileStorageService, new com.genifast.dms.service.util.WatermarkService());
        GlobalExceptionHandler advice = new GlobalExceptionHandler(auditLogService, userRepository);
        MockMvc mvc403 = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(advice)
                .build();

        Long docId = 110201L;
        Mockito.when(mockedService.approveDocument(eq(docId)))
                .thenThrow(new com.genifast.dms.common.exception.ApiException(
                        com.genifast.dms.common.constant.ErrorCode.ACCESS_DENIED,
                        "Access denied"
                ));

        mvc403.perform(MockMvcRequestBuilders.post("/api/v1/documents/{id}/approve", docId))
                .andExpect(status().isForbidden());
    }

    // [11.3] Phó khoa tải xuống tài liệu: trong quyền 200 OK, ngoài quyền/ABAC 403
    @Test
    @DisplayName("11.3 - GET /api/v1/documents/{id}/download - vice head within dept - 200 OK")
    void scenario113_Download_Ok_WithinDepartment() throws Exception {
        String email = "pho.khoa.cntt@genifast.edu.vn";
        authenticateAs(email, false, false, deptCNTT);

        Long docId = 110301L;
        Document doc = buildDoc(docId, 3, deptCNTT, 3, 2, "creator@genifast.edu.vn"); // confidentiality=2 (non-sensitive)
        when(documentRepository.findById(eq(docId))).thenReturn(Optional.of(doc));
        when(fileStorageService.loadAsResource(eq(doc.getFilePath()))).thenReturn(new ByteArrayResource("DATA".getBytes()));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/documents/{id}/download", docId))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", Matchers.containsString(doc.getOriginalFilename())));
    }

    @Test
    @DisplayName("11.3 - GET /api/v1/documents/{id}/download - vice head from external device - 403 Forbidden")
    void scenario113_Download_Forbidden_ExternalDeviceOnSensitiveDoc() throws Exception {
        String email = "pho.khoa.cntt@genifast.edu.vn";
        authenticateAs(email, false, false, deptCNTT);

        Long docId = 110302L;
        Document doc = buildDoc(docId, 3, deptTCHC, 3, 3, "creator@genifast.edu.vn"); // confidentiality=3 (sensitive)
        when(documentRepository.findById(eq(docId))).thenReturn(Optional.of(doc));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/documents/{id}/download", docId)
                        .header("Device-Type", "EXTERNAL_DEVICE"))
                .andExpect(status().isForbidden());
    }
}
