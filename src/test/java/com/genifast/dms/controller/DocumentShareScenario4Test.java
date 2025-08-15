package com.genifast.dms.controller;

import com.genifast.dms.dto.request.DocumentShareRequest;
import com.genifast.dms.service.DocumentService;
import com.genifast.dms.service.FileStorageService;
import com.genifast.dms.service.util.WatermarkService;
import com.genifast.dms.testutil.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static com.genifast.dms.testutil.TestData.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("DocumentController - Share Scenario 4")
class DocumentShareScenario4Test {

    private MockMvc mockMvc;

    @Mock
    private DocumentService documentService;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private WatermarkService watermarkService;

    @Mock
    private com.genifast.dms.service.AuditLogService auditLogService;

    @Mock
    private com.genifast.dms.repository.UserRepository userRepository;

    @BeforeEach
    void setup() {
        DocumentController controller = new DocumentController(documentService, fileStorageService, watermarkService);
        com.genifast.dms.common.handler.GlobalExceptionHandler advice =
                new com.genifast.dms.common.handler.GlobalExceptionHandler(auditLogService, userRepository);
        this.mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(advice)
                .build();
    }

    // =============================
    // KỊCH BẢN 4: Phó phòng (user-pp)
    //  - [4.1] INTERNAL, APPROVED doc-04; share to user-cb with shareable + timebound (2025-08-05 -> 2025-08-12) -> 201
    // =============================

    // [4.1] theo ai/kich ban.md - INTERNAL APPROVED doc-04 shareable + timebound -> 201 Created
    @Test
    @DisplayName("4.1 - POST /api/v1/documents/{id}/share - internal approved shareable + timebound - 201 Created")
    void scenario41_InternalApproved_Shareable_Timebound_Created() throws Exception {
        Long docId = DOC_SC4_SHAREABLE_TIMEBOUND; // đại diện doc-04 (INTERNAL, APPROVED)
        String body = TestData.buildShareBody(
                USER_CB,
                List.of(PERM_SHAREABLE, PERM_TIMEBOUND),
                TIME_FROM_SAMPLE,
                TIME_TO_SAMPLE,
                null
        );

        doNothing().when(documentService).shareDocument(eq(docId), any(DocumentShareRequest.class));

        mockMvc.perform(post("/api/v1/documents/{id}/share", docId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Tài liệu đã được chia sẻ thành công."));

        ArgumentCaptor<DocumentShareRequest> captor = ArgumentCaptor.forClass(DocumentShareRequest.class);
        verify(documentService).shareDocument(eq(docId), captor.capture());
        DocumentShareRequest sent = captor.getValue();
        assertThat(sent.getRecipientId()).isEqualTo(USER_CB);
        assertThat(sent.getPermissions()).contains(PERM_SHAREABLE, PERM_TIMEBOUND);
        assertThat(sent.getFromDate()).isNotNull();
        assertThat(sent.getToDate()).isNotNull();
        assertThat(Boolean.TRUE.equals(sent.getIsShareToExternal())).isFalse();
    }
}
