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
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import org.mockito.Mockito;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("DocumentController - Scenario 10 (RBAC Trưởng khoa)")
class DocumentScenario10Test {

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

    // [10.1] Trưởng khoa phê duyệt và phân phối tài liệu (APPROVE + DISTRIBUTE)
    @Test
    @DisplayName("10.1 - POST /api/v1/documents/{id}/approve then /distribute - dept manager - 200 OK")
    void scenario101_ApproveAndDistribute_Ok() throws Exception {
        String email = "truongkhoa.cntt@genifast.edu.vn";
        // Trưởng khoa K.CNTT (dept manager), KHÔNG phải org manager
        authenticateAs(email, false, true, deptCNTT);

        // doc thuộc K.CNTT, trạng thái PENDING (2) -> APPROVED (3)
        Long docId = 100201L;
        Document doc = buildDoc(docId, 3, deptCNTT, 2, 2);
        when(documentRepository.findById(eq(docId))).thenReturn(Optional.of(doc));
        // Khi approve, service sẽ save(doc) rồi mapper.toDocumentResponse(approvedDoc)
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

        // 1) APPROVE -> 200 OK với body DocumentResponse
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/documents/{id}/approve", docId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(docId));

        // 2) DISTRIBUTE -> 200 OK với body chuỗi thông báo
        // Controller nhận List<Long> departmentIds, khác với tài liệu test plan (recipients)
        List<Long> deptIds = List.of(deptCNTT.getId(), deptTCHC.getId());
        when(departmentRepository.findById(any())).thenAnswer(inv -> {
            Long id = inv.getArgument(0);
            if (id.equals(deptCNTT.getId())) return Optional.of(deptCNTT);
            if (id.equals(deptTCHC.getId())) return Optional.of(deptTCHC);
            return Optional.empty();
        });
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/documents/{id}/distribute", docId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[" + deptCNTT.getId() + "," + deptTCHC.getId() + "]"))
                .andExpect(status().isOk());
    }

    // [10.2] Trưởng khoa ký điện tử tài liệu nhưng KHÔNG có quyền -> 403 Forbidden
    @Test
    @DisplayName("10.2 - POST /api/v1/documents/{id}/sign - dept manager without permission - 403 Forbidden")
    void scenario102_Sign_Forbidden_WhenNoPermission() throws Exception {
        String email = "truongkhoa.cntt@genifast.edu.vn";
        authenticateAs(email, false, true, deptCNTT);

        Long docId = 100202L;
        Document doc = buildDoc(docId, 3, deptCNTT, 3, 3); // APPROVED, PRIVATE -> ABAC chặn nếu không phải COMPANY_DEVICE
        when(documentRepository.findById(eq(docId))).thenReturn(Optional.of(doc));

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/documents/{id}/sign", docId))
                .andExpect(status().isForbidden());
    }

    // [10.3] Upload tài liệu (multipart) - thành công 201 Created
    @Test
    @DisplayName("10.3 - POST /api/v1/documents (multipart) - dept manager - 201 Created")
    void scenario103_UploadDocument_Created() throws Exception {
        String email = "truongkhoa.cntt@genifast.edu.vn";
        authenticateAs(email, false, true, deptCNTT);

        // Category thuộc cùng tổ chức/phòng ban của user
        var cat = new com.genifast.dms.entity.Category();
        cat.setId(501L);
        cat.setName("Quy chế");
        cat.setDepartment(deptCNTT);
        cat.setOrganization(org);
        when(categoryRepository.findById(eq(501L))).thenReturn(Optional.of(cat));

        // Giả lập lưu file và document
        when(fileStorageService.store(any())).thenReturn("/uploads/cntt/quyche-1.pdf");
        when(documentRepository.save(any(Document.class))).thenAnswer(inv -> {
            Document d = inv.getArgument(0);
            d.setId(9001L);
            return d;
        });
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

        String metadataJson = "{" +
                "\"title\":\"Quy che hoc vu\"," +
                "\"content\":\"Noi dung\"," +
                "\"description\":\"Mo ta\"," +
                "\"category_id\":501," +
                "\"access_type\":3" +
                "}";

        MockMultipartFile metadata = new MockMultipartFile(
                "metadata", "metadata.json", "application/json", metadataJson.getBytes()
        );
        MockMultipartFile file = new MockMultipartFile(
                "file", "quyche.pdf", "application/pdf", "PDF-DATA".getBytes()
        );

        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/documents")
                        .file(metadata)
                        .file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(9001));
    }

    // [10.3] Upload tài liệu (multipart) - bị chặn 403 Forbidden (mô phỏng ném ApiException ACCESS_DENIED)
    @Test
    @DisplayName("10.3 - POST /api/v1/documents (multipart) - forbidden 403 when access denied")
    void scenario103_UploadDocument_Forbidden() throws Exception {
        String email = "truongkhoa.cntt@genifast.edu.vn";
        authenticateAs(email, false, true, deptCNTT);

        // Dựng controller riêng dùng DocumentService mock để ném ApiException ACCESS_DENIED
        DocumentService mockedService = Mockito.mock(DocumentService.class);
        DocumentController controller = new DocumentController(mockedService, fileStorageService, new com.genifast.dms.service.util.WatermarkService());
        GlobalExceptionHandler advice = new GlobalExceptionHandler(auditLogService, userRepository);
        MockMvc mvc403 = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(advice)
                .build();

        String metadataJson = "{" +
                "\"title\":\"Quy che hoc vu\"," +
                "\"content\":\"Noi dung\"," +
                "\"description\":\"Mo ta\"," +
                "\"category_id\":501," +
                "\"access_type\":3" +
                "}";
        MockMultipartFile metadata = new MockMultipartFile(
                "metadata", "metadata.json", "application/json", metadataJson.getBytes()
        );
        MockMultipartFile file = new MockMultipartFile(
                "file", "quyche.pdf", "application/pdf", "PDF-DATA".getBytes()
        );

        Mockito.when(mockedService.createDocument(any(String.class), any(org.springframework.web.multipart.MultipartFile.class)))
                .thenThrow(new com.genifast.dms.common.exception.ApiException(
                        com.genifast.dms.common.constant.ErrorCode.ACCESS_DENIED,
                        "Access denied"
                ));

        mvc403.perform(MockMvcRequestBuilders.multipart("/api/v1/documents").file(metadata).file(file))
                .andExpect(status().isForbidden());
    }
}
