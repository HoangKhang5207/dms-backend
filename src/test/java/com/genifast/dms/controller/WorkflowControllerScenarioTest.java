package com.genifast.dms.controller;

import com.genifast.dms.dto.request.workflow.WorkflowAssignDeptRequest;
import com.genifast.dms.dto.request.workflow.WorkflowAssignEleRequest;
import com.genifast.dms.dto.request.workflow.WorkflowDeployRequest;
import com.genifast.dms.dto.request.workflow.WorkflowEleDto;
import com.genifast.dms.dto.request.workflow.WorkflowStartRequest;
import com.genifast.dms.dto.response.workflow.DeployWorkflowResponse;
import com.genifast.dms.dto.response.workflow.ProcessTResponse;
import com.genifast.dms.service.workflow.WorkflowService;
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


import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("WorkflowController - Scenarios")
class WorkflowControllerScenarioTest {

    private MockMvc mockMvc;

    @Mock
    private WorkflowService workflowService;

    @BeforeEach
    void setup() {
        WorkflowController controller = new WorkflowController(workflowService);
        this.mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    // [WF.1] theo ai/KichBanKiemThuWorkflowModule.txt - TC_WF_01: Deploy workflow thành công
    @Test
    @DisplayName("WF.1 - POST /api/v1/workflows/deploy - 200 OK")
    void deployWorkflow_Success_200() throws Exception {
        DeployWorkflowResponse resp = new DeployWorkflowResponse("dep-123", 1L);
        when(workflowService.deploy(any(WorkflowDeployRequest.class))).thenReturn(resp);

        String body = "{" +
                "\"bpmnUploadId\":1," +
                "\"documentType\":\"1\"," +
                "\"name\":\"Quy trình phê duyệt công văn đi chuẩn\"," +
                "\"description\":\"Áp dụng cho các công văn đi thông thường\"}";

        mockMvc.perform(post("/api/v1/workflows/deploy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deploymentId").value("dep-123"))
                .andExpect(jsonPath("$.workflowId").value(1));

        ArgumentCaptor<WorkflowDeployRequest> captor = ArgumentCaptor.forClass(WorkflowDeployRequest.class);
        verify(workflowService).deploy(captor.capture());
        WorkflowDeployRequest sent = captor.getValue();
        assertThat(sent.getBpmnUploadId()).isEqualTo(1L);
        assertThat(sent.getDocumentType()).isEqualTo("1");
    }

    // [WF.2] theo ai/KichBanKiemThuWorkflowModule.txt - TC_WF_03: Assign departments
    @Test
    @DisplayName("WF.2 - POST /api/v1/workflows/assign_dept - 200 OK")
    void assignDepartments_Success_200() throws Exception {
        String body = "{" +
                "\"id\":1," +
                "\"departmentIds\":[1,2]}";

        mockMvc.perform(post("/api/v1/workflows/assign_dept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        ArgumentCaptor<WorkflowAssignDeptRequest> captor = ArgumentCaptor.forClass(WorkflowAssignDeptRequest.class);
        verify(workflowService).assignDepartments(captor.capture());
        WorkflowAssignDeptRequest sent = captor.getValue();
        assertThat(sent.getId()).isEqualTo(1L);
        assertThat(sent.getDepartmentIds()).containsExactlyInAnyOrder(1L, 2L);
    }

    // [WF.3] theo ai/KichBanKiemThuWorkflowModule.txt - TC_WF_04: Assign elements
    @Test
    @DisplayName("WF.3 - POST /api/v1/workflows/assign_ele - 200 OK")
    void assignElements_Success_200() throws Exception {
        String body = "{" +
                "\"id\":1," +
                "\"workflowEleDto\":{" +
                "\"categoryIds\":[10,11]," +
                "\"urgency\":[\"Bình thường\",\"Khẩn\"]," +
                "\"security\":[\"Nội bộ\",\"Mật\"]}}";

        mockMvc.perform(post("/api/v1/workflows/assign_ele")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        ArgumentCaptor<WorkflowAssignEleRequest> captor = ArgumentCaptor.forClass(WorkflowAssignEleRequest.class);
        verify(workflowService).assignElements(captor.capture());
        WorkflowAssignEleRequest sent = captor.getValue();
        assertThat(sent.getId()).isEqualTo(1L);
        WorkflowEleDto dto = sent.getWorkflowEleDto();
        assertThat(dto.getCategoryIds()).containsExactly(10L, 11L);
        assertThat(dto.getUrgency()).contains("Bình thường", "Khẩn");
        assertThat(dto.getSecurity()).contains("Nội bộ", "Mật");
    }

    // [EXEC.1] theo ai/KichBanKiemThuWorkflowModule.txt - TC_EXEC_01: Start workflow thành công
    @Test
    @DisplayName("EXEC.1 - POST /api/v1/workflows/start - 200 OK")
    void startWorkflow_Success_200() throws Exception {
        ProcessTResponse resp = ProcessTResponse.builder()
                .processKey("demo-approval")
                .taskKeyNext("Activity_TruongPhong")
                .processUser(2002L)
                .build();
        when(workflowService.start(any(WorkflowStartRequest.class))).thenReturn(resp);

        String body = "{" +
                "\"workflowId\":1," +
                "\"documentId\":100," +
                "\"condition\":\"DEFAULT\"," +
                "\"startUser\":1001," +
                "\"processUser\":2002}";

        mockMvc.perform(post("/api/v1/workflows/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processKey").value("demo-approval"))
                .andExpect(jsonPath("$.taskKeyNext").value("Activity_TruongPhong"))
                .andExpect(jsonPath("$.processUser").value(2002));

        ArgumentCaptor<WorkflowStartRequest> captor = ArgumentCaptor.forClass(WorkflowStartRequest.class);
        verify(workflowService).start(captor.capture());
        WorkflowStartRequest sent = captor.getValue();
        assertThat(sent.getWorkflowId()).isEqualTo(1L);
        assertThat(sent.getDocumentId()).isEqualTo(100L);
        assertThat(sent.getStartUser()).isEqualTo(1001L);
        assertThat(sent.getProcessUser()).isEqualTo(2002L);
    }
}
