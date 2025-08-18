package com.genifast.dms.controller;

import com.genifast.dms.dto.request.workflow.TaskProcessRequest;
import com.genifast.dms.dto.response.workflow.ProcessTResponse;
import com.genifast.dms.service.workflow.TaskService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("TaskController - Scenarios")
class TaskControllerScenarioTest {

    private MockMvc mockMvc;

    @Mock
    private TaskService taskService;

    @Mock
    private com.genifast.dms.service.AuditLogService auditLogService;

    @Mock
    private com.genifast.dms.repository.UserRepository userRepository;

    @BeforeEach
    void setup() {
        TaskController controller = new TaskController(taskService);
        com.genifast.dms.common.handler.GlobalExceptionHandler advice =
                new com.genifast.dms.common.handler.GlobalExceptionHandler(auditLogService, userRepository);
        this.mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(advice)
                .build();
    }

    // [EXEC.2] theo ai/KichBanKiemThuWorkflowModule.txt - TC_EXEC_02: Process task - APPROVE
    @Test
    @DisplayName("EXEC.2 - POST /api/v1/tasks/document/{documentId}/process - APPROVE - 200 OK")
    void processTask_Approve_200() throws Exception {
        Long documentId = 100L;
        ProcessTResponse resp = ProcessTResponse.builder()
                .processKey("demo-approval")
                .taskKeyNext("Activity_VanThu")
                .processUser(3003L)
                .build();
        when(taskService.process(eq(documentId), any(TaskProcessRequest.class))).thenReturn(resp);

        String body = "{" +
                "\"processUser\":3003," +
                "\"condition\":\"APPROVE\"}";

        mockMvc.perform(post("/api/v1/tasks/document/{documentId}/process", documentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taskKeyNext").value("Activity_VanThu"))
                .andExpect(jsonPath("$.processUser").value(3003));

        ArgumentCaptor<TaskProcessRequest> captor = ArgumentCaptor.forClass(TaskProcessRequest.class);
        org.mockito.Mockito.verify(taskService).process(eq(documentId), captor.capture());
        TaskProcessRequest sent = captor.getValue();
        assertThat(sent.getProcessUser()).isEqualTo(3003L);
        assertThat(sent.getCondition()).isEqualTo("APPROVE");
    }

    // [EXEC.3] theo ai/KichBanKiemThuWorkflowModule.txt - TC_EXEC_03: Process task - REJECT
    @Test
    @DisplayName("EXEC.3 - POST /api/v1/tasks/document/{documentId}/process - REJECT - 200 OK")
    void processTask_Reject_200() throws Exception {
        Long documentId = 100L;
        ProcessTResponse resp = ProcessTResponse.builder()
                .processKey("demo-approval")
                .taskKeyNext("Activity_KhoiTaoLai")
                .processUser(1001L)
                .build();
        when(taskService.process(eq(documentId), any(TaskProcessRequest.class))).thenReturn(resp);

        String body = "{" +
                "\"processUser\":1001," +
                "\"condition\":\"REJECT\"}";

        mockMvc.perform(post("/api/v1/tasks/document/{documentId}/process", documentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taskKeyNext").value("Activity_KhoiTaoLai"))
                .andExpect(jsonPath("$.processUser").value(1001));

        ArgumentCaptor<TaskProcessRequest> captor = ArgumentCaptor.forClass(TaskProcessRequest.class);
        org.mockito.Mockito.verify(taskService).process(eq(documentId), captor.capture());
        TaskProcessRequest sent = captor.getValue();
        assertThat(sent.getProcessUser()).isEqualTo(1001L);
        assertThat(sent.getCondition()).isEqualTo("REJECT");
    }

    // [EXEC.4] theo ai/KichBanKiemThuWorkflowModule.txt - TC_EXEC_04: Process task - Unauthorized (403)
    @Test
    @DisplayName("EXEC.4 - POST /api/v1/tasks/document/{documentId}/process - Unauthorized - 403")
    void processTask_Unauthorized_403() throws Exception {
        Long documentId = 100L;
        org.springframework.security.access.AccessDeniedException ex =
                new org.springframework.security.access.AccessDeniedException("Not allowed");
        when(taskService.process(eq(documentId), any(TaskProcessRequest.class))).thenThrow(ex);

        String body = "{" +
                "\"processUser\":9999," +
                "\"condition\":\"APPROVE\"}";

        mockMvc.perform(post("/api/v1/tasks/document/{documentId}/process", documentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").exists());
    }
}
