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

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("DocumentController - Scenario 8 (ABAC theo phòng ban)")
class DocumentScenario8Test {

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

    private void authenticateAs(String email) {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(email, "pwd"));
        when(userRepository.findByEmail(eq(email))).thenReturn(Optional.of(buildUser(email, false, false, deptCNTT)));
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

    // [8.1] Nội bộ - cùng phòng ban truy cập tài liệu do phòng ban mình tạo -> 200 OK
    @Test
    @DisplayName("8.1 - GET /api/v1/documents/{id} - same department - 200 OK")
    void scenario81_SameDept_Ok() throws Exception {
        String email = "giaovu.cntt@genifast.edu.vn";
        authenticateAs(email);

        Document doc = buildDoc(80201L, 3, deptCNTT, 3, 2); // accessType=Department, status=APPROVED, confidentiality=INTERNAL
        when(documentRepository.findById(eq(80201L))).thenReturn(Optional.of(doc));
        stubDocumentResponse(doc);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/documents/{id}", 80201L)
                        .header("Device-Type", "COMPANY_DEVICE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(80201L))
                .andExpect(jsonPath("$.access_type").value(3));
    }

    // [8.3] Hạn chế hành chính - khác phòng ban, từ thiết bị công ty -> 403 (không được chia sẻ)
    @Test
    @DisplayName("8.3 - GET /api/v1/documents/{id} - different department (company device) - 403")
    void scenario83_DiffDept_CompanyDevice_Forbidden() throws Exception {
        String email = "canbo.tchc@genifast.edu.vn";
        // User thuộc P.TCHC (không phải CNTT)
        User user = buildUser(email, false, false, deptTCHC);
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(email, "pwd"));
        when(userRepository.findByEmail(eq(email))).thenReturn(Optional.of(user));

        Document doc = buildDoc(80301L, 3, deptCNTT, 3, 2); // Department scope, khác phòng ban
        when(documentRepository.findById(eq(80301L))).thenReturn(Optional.of(doc));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/documents/{id}", 80301L)
                        .header("Device-Type", "COMPANY_DEVICE"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403001))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.path").value("/api/v1/documents/80301"));
    }

    // [8.3] Hạn chế hành chính - thiết bị bên ngoài -> 403 theo rule thiết bị
    @Test
    @DisplayName("8.3 - GET /api/v1/documents/{id} - external device - 403 (device rule)")
    void scenario83_ExternalDevice_Forbidden() throws Exception {
        String email = "canbo.tchc@genifast.edu.vn";
        User user = buildUser(email, false, false, deptTCHC);
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(email, "pwd"));
        when(userRepository.findByEmail(eq(email))).thenReturn(Optional.of(user));

        // confidentiality=3 (PRIVATE/LOCKED scope cho rule thiết bị), accessType=2 (tổ chức) để chỉ test thiết bị
        Document doc = buildDoc(80302L, 2, deptCNTT, 3, 3);
        when(documentRepository.findById(eq(80302L))).thenReturn(Optional.of(doc));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/documents/{id}", 80302L)
                        .header("Device-Type", "EXTERNAL_DEVICE"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403001))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("Access denied from external device for private/locked document."))
                .andExpect(jsonPath("$.path").value("/api/v1/documents/80302"));
    }

    // [8.2] Phòng ban truy cập tài liệu được chia sẻ (doc-02: DEPARTMENT scope, user khác phòng ban nhưng đã được share) -> 200 OK
    @Test
    @DisplayName("8.2 - GET /api/v1/documents/{id} - department shared to other user - 200 OK")
    void scenario82_DepartmentShared_Ok() throws Exception {
        // user-pp thuộc P.DTAO
        String email = "phophong.dtao@genifast.edu.vn";
        User user = buildUser(email, false, true, deptTCHC); // giả định pp thuộc TCHC trong test
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(email, "pwd"));
        when(userRepository.findByEmail(eq(email))).thenReturn(Optional.of(user));

        // doc-02 thuộc K.CNTT, accessType=3 (DEPARTMENT), status=APPROVED
        Document doc = buildDoc(80202L, 3, deptCNTT, 3, 2);
        when(documentRepository.findById(eq(80202L))).thenReturn(Optional.of(doc));

        // ĐÃ ĐƯỢC CHIA SẺ: tồn tại bản ghi trong private_docs cho user-pp
        when(privateDocumentRepository.findByUserAndDocumentAndStatus(eq(user), eq(doc), eq(1)))
                .thenReturn(Optional.of(new com.genifast.dms.entity.PrivateDoc()));

        stubDocumentResponse(doc);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/documents/{id}", 80202L)
                        .header("Device-Type", "COMPANY_DEVICE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(80202L))
                .andExpect(jsonPath("$.access_type").value(3));
    }

    // [8.4] PRIVATE - user được thêm vào private_docs -> 200 OK
    @Test
    @DisplayName("8.4 - GET /api/v1/documents/{id} - private (authorized) - 200 OK")
    void scenario84_Private_Authorized_Ok() throws Exception {
        String email = "phophong.dtao@genifast.edu.vn";
        authenticateAs(email);
        User current = userRepository.findByEmail(email).orElseThrow();

        Document doc = buildDoc(80401L, 4, deptCNTT, 3, 3);
        when(documentRepository.findById(eq(80401L))).thenReturn(Optional.of(doc));
        // được cấp quyền trong private_docs
        when(privateDocumentRepository.findByUserAndDocumentAndStatus(eq(current), eq(doc), eq(1)))
                .thenReturn(Optional.of(new com.genifast.dms.entity.PrivateDoc()));
        stubDocumentResponse(doc);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/documents/{id}", 80401L)
                        .header("Device-Type", "COMPANY_DEVICE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(80401L));
    }

    // [8.5] Cấp bậc quản lý - BGH truy cập tài liệu của phòng ban khác -> 200 OK
    @Test
    @DisplayName("8.5 - GET /api/v1/documents/{id} - organization manager - 200 OK")
    void scenario85_OrgManager_Ok() throws Exception {
        String email = "hieutruong@genifast.edu.vn";
        User user = buildUser(email, true, false, deptTCHC);
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(email, "pwd"));
        when(userRepository.findByEmail(eq(email))).thenReturn(Optional.of(user));

        // INTERNAL doc (confidentiality=2), accessType=2 (tổ chức)
        Document doc = buildDoc(80501L, 2, deptCNTT, 3, 2);
        when(documentRepository.findById(eq(80501L))).thenReturn(Optional.of(doc));
        stubDocumentResponse(doc);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/documents/{id}", 80501L)
                        .header("Device-Type", "COMPANY_DEVICE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(80501L));
    }

    // [8.6] Chia sẻ tài liệu DRAFT -> 403 với thông báo chuẩn
    @Test
    @DisplayName("8.6 - POST /api/v1/documents/{id}/share - DRAFT - 403")
    void scenario86_ShareDraft_Forbidden() throws Exception {
        String email = "chuyenvien.dtao@genifast.edu.vn";
        authenticateAs(email);

        Document draftDoc = buildDoc(80601L, 2, deptCNTT, 1, 2); // status=1 DRAFT
        when(documentRepository.findById(eq(80601L))).thenReturn(Optional.of(draftDoc));

        // Mapper không dùng. Chỉ cần verify response lỗi 403001
        String body = "{\n  \"recipientEmail\": \"giaovu.cntt@genifast.edu.vn\",\n  \"isShareToExternal\": false\n}";

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/documents/{id}/share", 80601L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403001))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("Document is not in APPROVED status."))
                .andExpect(jsonPath("$.path").value("/api/v1/documents/80601/share"));
    }
}
