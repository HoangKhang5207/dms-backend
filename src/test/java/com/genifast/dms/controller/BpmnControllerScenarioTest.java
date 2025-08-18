package com.genifast.dms.controller;

import com.genifast.dms.dto.response.workflow.BpmnUploadResponse;
import com.genifast.dms.service.workflow.BpmnService;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.filter.CharacterEncodingFilter;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("BpmnController - Scenarios")
class BpmnControllerScenarioTest {

    private MockMvc mockMvc;

    @Mock
    private BpmnService bpmnService;

    @Mock
    private com.genifast.dms.service.AuditLogService auditLogService;

    @Mock
    private com.genifast.dms.repository.UserRepository userRepository;

    @BeforeEach
    void setup() {
        BpmnController controller = new BpmnController(bpmnService);
        com.genifast.dms.common.handler.GlobalExceptionHandler advice =
                new com.genifast.dms.common.handler.GlobalExceptionHandler(auditLogService, userRepository);
        this.mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(advice)
                .addFilters(new CharacterEncodingFilter("UTF-8", true))
                .build();
    }

    // [BPMN.1] from ai/KichBanKiemThuWorkflowModule.txt - TC_BPMN_01: Upload BPMN successfully
    @Test
    @DisplayName("BPMN.1 - POST /api/v1/bpmn/organization/{orgId}/save - upload success - 201")
    void uploadBpmn_Success_201() throws Exception {
        Long orgId = 1L;
        MockMultipartFile bpmn = new MockMultipartFile("file", "QuyTrinhPheDuyet3Buoc.bpmn", "text/xml", "<xml></xml>".getBytes());
        MockMultipartFile svg = new MockMultipartFile("svgFile", "QuyTrinhPheDuyet3Buoc.svg", "image/svg+xml", "<svg></svg>".getBytes());
        MockMultipartFile name = new MockMultipartFile("name", "", "text/plain", "Internal Document Approval Process".getBytes(StandardCharsets.UTF_8));

        BpmnUploadResponse resp = new BpmnUploadResponse();
        resp.setId(1L);
        resp.setName("Internal Document Approval Process");
        resp.setPath("https://blob/bpmn?sig=abc");
        resp.setPathSvg("https://blob/svg?sig=xyz");
        resp.setVersion(0);
        resp.setIsDeployed(false);
        resp.setOrganizationId(1L);
        org.mockito.Mockito.doReturn(resp)
                .when(bpmnService)
                .saveBpmn(any(Long.class), any(String.class),
                        any(org.springframework.web.multipart.MultipartFile.class),
                        any(org.springframework.web.multipart.MultipartFile.class),
                        org.mockito.ArgumentMatchers.nullable(Long.class), any(Boolean.class));

        mockMvc.perform(multipart("/api/v1/bpmn/organization/{organizationId}/save", orgId)
                        .file(bpmn)
                        .file(svg)
                        .file(name)
                        .param("isPublished", "false")
                        .characterEncoding("UTF-8")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Internal Document Approval Process"))
                .andExpect(jsonPath("$.version").value(0));

        ArgumentCaptor<String> nameCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<org.springframework.web.multipart.MultipartFile> bpmnCaptor = ArgumentCaptor.forClass(org.springframework.web.multipart.MultipartFile.class);
        ArgumentCaptor<org.springframework.web.multipart.MultipartFile> svgCaptor = ArgumentCaptor.forClass(org.springframework.web.multipart.MultipartFile.class);
        ArgumentCaptor<Long> idCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Boolean> publishedCaptor = ArgumentCaptor.forClass(Boolean.class);

        org.mockito.Mockito.verify(bpmnService).saveBpmn(eq(orgId), nameCaptor.capture(), bpmnCaptor.capture(), svgCaptor.capture(), idCaptor.capture(), publishedCaptor.capture());
        assertThat(nameCaptor.getValue()).isEqualTo("Internal Document Approval Process");
        assertThat(publishedCaptor.getValue()).isFalse();
    }

    // [BPMN.2] theo ai/KichBanKiemThuWorkflowModule.txt - TC_BPMN_02: Upload thiếu file -> 400
    @Test
    @DisplayName("BPMN.2 - POST /api/v1/bpmn/organization/{orgId}/save - thiếu file - 400")
    void uploadBpmn_MissingFile_400() throws Exception {
        Long orgId = 1L;
        MockMultipartFile name = new MockMultipartFile("name", "", "text/plain", "Invalid Process".getBytes());

        mockMvc.perform(multipart("/api/v1/bpmn/organization/{organizationId}/save", orgId)
                        .file(name)
                        .param("isPublished", "false")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest());
    }

    // [BPMN.3] theo ai/KichBanKiemThuWorkflowModule.txt - TC_BPMN_03: List theo tổ chức -> 200
    @Test
    @DisplayName("BPMN.3 - GET /api/v1/bpmn/organization/{orgId} - 200")
    void listBpmnByOrg_Success_200() throws Exception {
        Long orgId = 1L;
        BpmnUploadResponse item = new BpmnUploadResponse();
        item.setId(1L);
        item.setName("Process A");
        item.setPath("p");
        item.setPathSvg("s");
        item.setVersion(0);
        item.setIsDeployed(false);
        item.setOrganizationId(orgId);
        java.util.List<BpmnUploadResponse> list = java.util.Arrays.asList(item);
        when(bpmnService.listByOrganization(eq(orgId))).thenReturn(list);

        mockMvc.perform(get("/api/v1/bpmn/organization/{organizationId}", orgId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Process A"));
    }

    // [BPMN.4] from ai/KichBanKiemThuWorkflowModule.txt - TC_BPMN_04: Update BPMN not deployed -> 201
    @Test
    @DisplayName("BPMN.4 - POST /api/v1/bpmn/organization/{orgId}/save - update not deployed - 201")
    void updateBpmn_NotDeployed_201() throws Exception {
        Long orgId = 1L;
        MockMultipartFile bpmn = new MockMultipartFile("file", "QuyTrinhPheDuyet_V2.bpmn", "text/xml", "<xml></xml>".getBytes());
        MockMultipartFile svg = new MockMultipartFile("svgFile", "QuyTrinhPheDuyet_V2.svg", "image/svg+xml", "<svg></svg>".getBytes());
        MockMultipartFile name = new MockMultipartFile("name", "", "text/plain", "Approval Process V2".getBytes(StandardCharsets.UTF_8));

        BpmnUploadResponse resp = new BpmnUploadResponse();
        resp.setId(1L);
        resp.setName("Approval Process V2");
        resp.setPath("p2");
        resp.setPathSvg("s2");
        resp.setVersion(1);
        resp.setIsDeployed(false);
        resp.setOrganizationId(orgId);
        org.mockito.Mockito.doReturn(resp)
                .when(bpmnService)
                .saveBpmn(any(Long.class), any(String.class),
                        any(org.springframework.web.multipart.MultipartFile.class),
                        any(org.springframework.web.multipart.MultipartFile.class),
                        any(Long.class), any(Boolean.class));

        mockMvc.perform(multipart("/api/v1/bpmn/organization/{organizationId}/save", orgId)
                        .file(bpmn)
                        .file(svg)
                        .file(name)
                        .param("bpmnUploadId", "1")
                        .param("isPublished", "false")
                        .characterEncoding("UTF-8")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.name").value("Approval Process V2"));
    }

    // [BPMN.5] from ai/KichBanKiemThuWorkflowModule.txt - TC_BPMN_05: Update BPMN deployed -> 400
    @Test
    @DisplayName("BPMN.5 - POST /api/v1/bpmn/organization/{orgId}/save - update deployed - 400")
    void updateBpmn_Deployed_400() throws Exception {
        Long orgId = 1L;
        MockMultipartFile bpmn = new MockMultipartFile("file", "Any.bpmn", "text/xml", "<xml></xml>".getBytes());
        MockMultipartFile name = new MockMultipartFile("name", "", "text/plain", "Try updating deployed process".getBytes(StandardCharsets.UTF_8));

        org.mockito.Mockito.doThrow(new IllegalArgumentException("Cannot update or upload files for a deployed BPMN"))
                .when(bpmnService)
                .saveBpmn(any(Long.class), any(String.class),
                        any(org.springframework.web.multipart.MultipartFile.class),
                        org.mockito.ArgumentMatchers.nullable(org.springframework.web.multipart.MultipartFile.class),
                        any(Long.class), any(Boolean.class));

        mockMvc.perform(multipart("/api/v1/bpmn/organization/{organizationId}/save", orgId)
                        .file(bpmn)
                        .file(name)
                        .param("bpmnUploadId", "2")
                        .param("isPublished", "false")
                        .characterEncoding("UTF-8")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    // [BPMN.6] from ai/KichBanKiemThuWorkflowModule.txt - TC_BPMN_06: Delete BPMN not deployed -> 204
    @Test
    @DisplayName("BPMN.6 - DELETE /api/v1/bpmn/organization/{orgId}/bpmn_upload/{id} - 204")
    void deleteBpmn_NotDeployed_204() throws Exception {
        Long orgId = 1L;
        Long id = 3L;
        mockMvc.perform(delete("/api/v1/bpmn/organization/{organizationId}/bpmn_upload/{id}", orgId, id))
                .andExpect(status().isNoContent());
        org.mockito.Mockito.verify(bpmnService).softDelete(eq(orgId), eq(id));
    }

    // [BPMN.7] theo ai/KichBanKiemThuWorkflowModule.txt - TC_BPMN_07: Xóa BPMN đã deploy -> 400
    @Test
    @DisplayName("BPMN.7 - DELETE /api/v1/bpmn/organization/{orgId}/bpmn_upload/{id} - deployed - 400")
    void deleteBpmn_Deployed_400() throws Exception {
        Long orgId = 1L;
        Long id = 2L;
        org.mockito.Mockito.doThrow(new IllegalArgumentException("Cannot delete a deployed BPMN"))
                .when(bpmnService).softDelete(eq(orgId), eq(id));

        mockMvc.perform(delete("/api/v1/bpmn/organization/{organizationId}/bpmn_upload/{id}", orgId, id))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }
}
