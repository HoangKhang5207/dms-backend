package com.genifast.dms.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.genifast.dms.common.constant.ErrorCode;
import com.genifast.dms.common.exception.ApiException;
import com.genifast.dms.common.handler.GlobalExceptionHandler;
import com.genifast.dms.dto.request.DelegationRequest;
import com.genifast.dms.dto.response.DelegationResponse;
import com.genifast.dms.dto.response.DocumentResponse;
import com.genifast.dms.repository.UserRepository;
import com.genifast.dms.service.DelegationService;
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

import java.time.Instant;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class DocumentScenario16Test {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock private DelegationService delegationService;
    @Mock private DocumentService documentService;
    @Mock private FileStorageService fileStorageService;
    @Mock private WatermarkService watermarkService;
    @Mock private UserRepository userRepository;

    @BeforeEach
    void setup() {
        DelegationController delegationController = new DelegationController(delegationService);
        DocumentController documentController = new DocumentController(documentService, fileStorageService, watermarkService);
        GlobalExceptionHandler handler = new GlobalExceptionHandler(null, userRepository);
        mockMvc = MockMvcBuilders.standaloneSetup(delegationController, documentController)
                .setControllerAdvice(handler)
                .build();

        // Giả lập đăng nhập - sẽ thay đổi email theo từng tình huống qua comments
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("test@genifast.edu.vn", "password", "delegate_process", "documents:approve", "documents:distribute"));
    }

    // [16.1] Hiệu trưởng ủy quyền phê duyệt tài liệu -> 201
    @Test
    @DisplayName("[16.1] Hiệu trưởng ủy quyền documents:approve cho Trưởng khoa (201)")
    void scenario161_DelegationApproveByPrincipal() throws Exception {
        DelegationResponse response = DelegationResponse.builder()
                .id(100L)
                .delegatorId(10L) // user-ht
                .delegateeId(20L) // user-tk
                .documentId(1L)   // doc-01
                .permission("documents:approve")
                .expiryAt(Instant.parse("2025-08-14T23:59:59Z"))
                .createdAt(Instant.now())
                .build();
        Mockito.when(delegationService.createDelegation(ArgumentMatchers.any(DelegationRequest.class)))
                .thenReturn(response);

        String body = "{" +
                "\"delegatee_id\":20," +
                "\"document_id\":1," +
                "\"permission\":\"documents:approve\"," +
                "\"start_at\":\"2025-08-07T00:00:00Z\"," +
                "\"expiry_at\":\"2025-08-14T23:59:59Z\"" +
                "}";

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/delegations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.permission").value("documents:approve"))
                .andExpect(jsonPath("$.document_id").value(1));
    }

    // [16.2] Trưởng khoa ủy quyền phân phối tài liệu -> 201
    @Test
    @DisplayName("[16.2] Trưởng khoa ủy quyền documents:distribute cho Phó khoa (201)")
    void scenario162_DelegationDistributeByDean() throws Exception {
        DelegationResponse response = DelegationResponse.builder()
                .id(101L)
                .delegatorId(20L) // user-tk
                .delegateeId(30L) // user-pk
                .documentId(2L)   // doc-02
                .permission("documents:distribute")
                .expiryAt(Instant.parse("2025-08-10T23:59:59Z"))
                .createdAt(Instant.now())
                .build();
        Mockito.when(delegationService.createDelegation(ArgumentMatchers.any(DelegationRequest.class)))
                .thenReturn(response);

        String body = "{" +
                "\"delegatee_id\":30," +
                "\"document_id\":2," +
                "\"permission\":\"documents:distribute\"," +
                "\"start_at\":\"2025-08-07T00:00:00Z\"," +
                "\"expiry_at\":\"2025-08-10T23:59:59Z\"" +
                "}";

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/delegations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.permission").value("documents:distribute"))
                .andExpect(jsonPath("$.document_id").value(2));
    }

    // [16.3] Chuyên viên ủy quyền ngoài quyền -> 403
    @Test
    @DisplayName("[16.3] Chuyên viên ủy quyền (ngoài quyền) bị chặn 403")
    void scenario163_DelegationForbiddenByStaff() throws Exception {
        Mockito.when(delegationService.createDelegation(ArgumentMatchers.any(DelegationRequest.class)))
                .thenThrow(new ApiException(ErrorCode.ACCESS_DENIED, "Bạn không có quyền thực hiện hành động này."));

        String body = "{" +
                "\"delegatee_id\":40," + // user-gv
                "\"document_id\":1," +  // doc-01
                "\"permission\":\"documents:approve\"," +
                "\"start_at\":\"2025-08-07T00:00:00Z\"" +
                "}";

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/delegations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message", Matchers.containsString("Bạn không có quyền")));
    }

    // [16.4] Trước khi ủy quyền: phó khoa approve doc-03 -> 403
    @Test
    @DisplayName("[16.4] Phó khoa phê duyệt trước khi được ủy quyền (403)")
    void scenario164_ApproveBeforeDelegation_Forbidden() throws Exception {
        Long docId = 3L; // doc-03
        Mockito.when(documentService.approveDocument(docId))
                .thenThrow(new ApiException(ErrorCode.ACCESS_DENIED, "Bạn không có quyền thực hiện hành động này."));

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/documents/{id}/approve", docId))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    // [16.5] Thực hiện ủy quyền -> 201
    @Test
    @DisplayName("[16.5] Trưởng khoa tạo ủy quyền approve cho Phó khoa (201)")
    void scenario165_CreateDelegationForDoc03() throws Exception {
        DelegationResponse response = DelegationResponse.builder()
                .id(102L)
                .delegatorId(20L)
                .delegateeId(30L)
                .documentId(3L)
                .permission("documents:approve")
                .expiryAt(Instant.parse("2025-08-11T23:59:59Z"))
                .createdAt(Instant.now())
                .build();
        Mockito.when(delegationService.createDelegation(ArgumentMatchers.any(DelegationRequest.class)))
                .thenReturn(response);

        String body = "{" +
                "\"delegatee_id\":30," +
                "\"document_id\":3," +
                "\"permission\":\"documents:approve\"," +
                "\"start_at\":\"2025-08-05T00:00:00Z\"," +
                "\"expiry_at\":\"2025-08-11T23:59:59Z\"" +
                "}";

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/delegations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.document_id").value(3))
                .andExpect(jsonPath("$.permission").value("documents:approve"));
    }

    // [16.6] Trong thời hạn ủy quyền: approve doc-03 -> 200
    @Test
    @DisplayName("[16.6] Phó khoa phê duyệt trong thời hạn ủy quyền (200)")
    void scenario166_ApproveWithinDelegation_Success() throws Exception {
        Long docId = 3L; // doc-03
        DocumentResponse resp = new DocumentResponse();
        resp.setId(docId);
        resp.setStatus(1); // giả sử 1 = APPROVED
        Mockito.when(documentService.approveDocument(docId)).thenReturn(resp);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/documents/{id}/approve", docId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(3));
    }

    // [16.7] Ngoài phạm vi ủy quyền: approve doc-02 -> 403
    @Test
    @DisplayName("[16.7] Phó khoa phê duyệt ngoài phạm vi ủy quyền (403)")
    void scenario167_ApproveOutOfScope_Forbidden() throws Exception {
        Long docId = 2L; // doc-02
        Mockito.when(documentService.approveDocument(docId))
                .thenThrow(new ApiException(ErrorCode.ACCESS_DENIED, "Bạn không có quyền thực hiện hành động này."));

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/documents/{id}/approve", docId))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    // [16.8] Hết hạn ủy quyền: approve doc-03 -> 403
    @Test
    @DisplayName("[16.8] Phó khoa phê duyệt sau khi hết hạn ủy quyền (403)")
    void scenario168_ApproveAfterExpiry_Forbidden() throws Exception {
        Long docId = 3L; // doc-03
        Mockito.when(documentService.approveDocument(docId))
                .thenThrow(new ApiException(ErrorCode.ACCESS_DENIED, "Bạn không có quyền thực hiện hành động này."));

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/documents/{id}/approve", docId))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }
}
