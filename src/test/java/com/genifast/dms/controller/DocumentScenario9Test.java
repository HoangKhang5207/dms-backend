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
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.isEmptyOrNullString;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("DocumentController - Scenario 9 (RBAC Hiệu trưởng)")
class DocumentScenario9Test {

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

        // reset security context
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(String email, boolean isOrgManager) {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(email, "pwd"));
        when(userRepository.findByEmail(eq(email))).thenReturn(Optional.of(buildUser(email, isOrgManager, false, deptTCHC)));
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

    private Document buildDoc(Long id, int accessType, Department dept, int status, int confidentiality) {
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
                .build();
        d.setContentType("application/pdf");
        d.setCreatedBy("creator@genifast.edu.vn");
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

    // [9.1] Hiệu trưởng ký điện tử tài liệu (SIGN)
    @Test
    @DisplayName("9.1 - POST /api/v1/documents/{id}/sign - organization manager - 201 Created")
    void scenario91_SignDocument_Ok() throws Exception {
        String email = "hieutruong@genifast.edu.vn";
        authenticateAs(email, true);

        Document doc = buildDoc(90401L, 2, deptCNTT, 3, 2);
        when(documentRepository.findById(eq(90401L))).thenReturn(Optional.of(doc));

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/documents/{id}/sign", 90401L))
                .andExpect(status().isCreated())
                .andExpect(content().string(containsString("được ký")));
    }

    // [9.2] Hiệu trưởng khóa tài liệu (LOCK)
    @Test
    @DisplayName("9.2 - POST /api/v1/documents/{id}/lock - organization manager - 201 Created")
    void scenario92_LockDocument_Ok() throws Exception {
        String email = "hieutruong@genifast.edu.vn";
        authenticateAs(email, true);

        Document doc = buildDoc(90201L, 2, deptCNTT, 3, 2);
        when(documentRepository.findById(eq(90201L))).thenReturn(Optional.of(doc));

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/documents/{id}/lock", 90201L))
                .andExpect(status().isCreated())
                .andExpect(content().string(containsString("được khóa")));
    }

    // [9.4] Hiệu trưởng công khai tài liệu (PUBLISH)
    @Test
    @DisplayName("9.4 - POST /api/v1/documents/{id}/publish - organization manager - 201 Created")
    void scenario94_PublishDocument_Ok() throws Exception {
        String email = "hieutruong@genifast.edu.vn";
        authenticateAs(email, true);

        Document doc = buildDoc(90901L, 1, deptCNTT, 2, 1); // values don't affect controller response in this test
        when(documentRepository.findById(eq(90901L))).thenReturn(Optional.of(doc));

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/documents/{id}/publish", 90901L))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value(containsString("công khai")));
    }

    // [9.5] Hiệu trưởng gửi thông báo (NOTIFY)
    @Test
    @DisplayName("9.5 - POST /api/v1/documents/{id}/notify - organization manager - 200 OK")
    void scenario95_NotifyDocument_Ok() throws Exception {
        String email = "hieutruong@genifast.edu.vn";
        authenticateAs(email, true);

        Document doc = buildDoc(90501L, 2, deptCNTT, 3, 2);
        when(documentRepository.findById(eq(90501L))).thenReturn(Optional.of(doc));

        String body = "Thông báo";
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/documents/{id}/notify", 90501L)
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content().string(not(isEmptyOrNullString())));
    }

    // [9.6] Hiệu trưởng mở khóa tài liệu (UNLOCK)
    @Test
    @DisplayName("9.6 - POST /api/v1/documents/{id}/unlock - organization manager - 201 Created")
    void scenario96_UnlockDocument_Ok() throws Exception {
        String email = "hieutruong@genifast.edu.vn";
        authenticateAs(email, true);

        Document doc = buildDoc(90601L, 2, deptCNTT, 3, 3);
        when(documentRepository.findById(eq(90601L))).thenReturn(Optional.of(doc));

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/documents/{id}/unlock", 90601L))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value(containsString("mở khóa")));
    }

    // [9.7] Hiệu trưởng đọc tài liệu PRIVATE với ABAC thiết bị
    @Test
    @DisplayName("9.7 - GET /api/v1/documents/{id} - private by org manager: company device 200, external 403")
    void scenario97_ReadPrivate_WithDeviceRules() throws Exception {
        String email = "hieutruong@genifast.edu.vn";
        authenticateAs(email, true);

        // Doc PRIVATE, APPROVED
        Document doc = buildDoc(90701L, 4, deptCNTT, 3, 3);
        when(documentRepository.findById(eq(90701L))).thenReturn(Optional.of(doc));
        // Cho phép org manager đọc: trong impl có nhánh org manager -> ok
        stubDocumentResponse(doc);

        // B1: thiết bị công ty -> 200 OK
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/documents/{id}", 90701L)
                        .header("Device-Type", "COMPANY_DEVICE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(90701L));

        // B2: thiết bị ngoài -> 403
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/documents/{id}", 90701L)
                        .header("Device-Type", "EXTERNAL_DEVICE"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403001))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("Access denied from external device for private/locked document."))
                .andExpect(jsonPath("$.path").value("/api/v1/documents/90701"));
    }
}
