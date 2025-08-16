package com.genifast.dms.controller;

import com.genifast.dms.service.AuditLogService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Scenario 9.3 - AuditLogController Security")
class AuditLogScenario9IT {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuditLogService auditLogService;

    // [9.3] Người dùng KHÔNG có authority 'audit:log' -> 403 Forbidden
    @Test
    @WithMockUser(username = "user@genifast.edu.vn", authorities = {"user:basic"})
    @DisplayName("9.3 - GET /api/v1/audit-logs - no authority -> 403")
    void getAuditLogs_Forbidden_WhenMissingAuthority() throws Exception {
        mockMvc.perform(get("/api/v1/audit-logs"))
                .andExpect(status().isForbidden());
    }

    // [9.3] Người dùng CÓ authority 'audit:log' -> 200 OK
    @Test
    @WithMockUser(username = "hieutruong@genifast.edu.vn", authorities = {"audit:log"})
    @DisplayName("9.3 - GET /api/v1/audit-logs - with authority -> 200")
    void getAuditLogs_Ok_WhenHasAuthority() throws Exception {
        when(auditLogService.getLogs(any(Pageable.class))).thenReturn(Page.empty());
        mockMvc.perform(get("/api/v1/audit-logs"))
                .andExpect(status().isOk());
    }
}
