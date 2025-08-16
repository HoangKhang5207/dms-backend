package com.genifast.dms.controller;

import com.genifast.dms.common.constant.ErrorCode;
import com.genifast.dms.common.exception.ApiException;
import com.genifast.dms.common.handler.GlobalExceptionHandler;
import com.genifast.dms.dto.request.DocumentUpdateRequest;
import com.genifast.dms.dto.request.ProjectRoleRequest;
import com.genifast.dms.dto.response.DocumentResponse;
import com.genifast.dms.dto.response.ProjectResponse;
import com.genifast.dms.dto.response.ProjectRoleResponse;
import com.genifast.dms.repository.UserRepository;
import com.genifast.dms.service.DocumentService;
import com.genifast.dms.service.FileStorageService;
import com.genifast.dms.service.ProjectService;
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

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class DocumentScenario17Test {

    private MockMvc mockMvc;

    @Mock private DocumentService documentService;
    @Mock private FileStorageService fileStorageService;
    @Mock private WatermarkService watermarkService;
    @Mock private ProjectService projectService;
    @Mock private UserRepository userRepository;

    @BeforeEach
    void setup() {
        DocumentController documentController = new DocumentController(documentService, fileStorageService, watermarkService);
        ProjectController projectController = new ProjectController(projectService);
        GlobalExceptionHandler handler = new GlobalExceptionHandler(null, userRepository);
        mockMvc = MockMvcBuilders.standaloneSetup(documentController, projectController)
                .setControllerAdvice(handler)
                .build();

        // Giả lập đăng nhập user-cv theo bối cảnh 17
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("chuyenvien.dtao@genifast.edu.vn", "password", "ROLE_MEMBER"));
    }

    // [17.1] Thao tác ngoài quyền dự án (Thất bại) -> 403
    @Test
    @DisplayName("[17.1] user-cv cập nhật tài liệu khi chưa có quyền dự án (403)")
    void scenario171_UpdateDocumentWithoutProjectPermission_Forbidden() throws Exception {
        Long docId = 1001L; // mapping cho doc-proj-01
        Mockito.when(documentService.updateDocumentMetadata(ArgumentMatchers.eq(docId), ArgumentMatchers.any(DocumentUpdateRequest.class)))
                .thenThrow(new ApiException(ErrorCode.ACCESS_DENIED, "Bạn không có quyền thực hiện hành động này."));

        String body = "{" +
                "\"title\":\"Cập nhật nội dung\"," +
                "\"description\":\"Mô tả\"" +
                "}";

        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/documents/{id}", docId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message", Matchers.containsString("Bạn không có quyền")));
    }

    // [17.2] Quản lý thành viên bởi Trưởng dự án (Thành công) -> 201, 200
    @Test
    @DisplayName("[17.2] Trưởng dự án tạo role và gán role cho user-cv (201, 200)")
    void scenario172_ProjectRoleCreateAndAssign_Success() throws Exception {
        Long projectId = 200L; // project-dms
        Long newRoleId = 300L; // prole-deputy
        Long userCvId = 400L;  // user-cv

        ProjectRoleResponse roleResp = new ProjectRoleResponse();
        // set tối thiểu các trường cần thiết
        // Do @Data không có builder, chỉ cần trả object non-null là đủ serialize
        Mockito.when(projectService.createProjectRole(ArgumentMatchers.eq(projectId), ArgumentMatchers.any(ProjectRoleRequest.class)))
                .thenReturn(roleResp);

        ProjectResponse projResp = new ProjectResponse();
        Mockito.when(projectService.changeMemberRole(projectId, userCvId, newRoleId)).thenReturn(projResp);

        String createRoleBody = "{" +
                "\"name\":\"Tổ phó\"," +
                "\"description\":\"Hỗ trợ trưởng dự án, được quyền cập nhật tài liệu.\"," +
                "\"permissionIds\":[1,2,3]" +
                "}";

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/projects/{projectId}/roles", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRoleBody))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));

        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/projects/{projectId}/members/{userId}/role/{newRoleId}", projectId, userCvId, newRoleId))
                .andExpect(status().isOk());
    }

    // [17.3] Thao tác trong quyền dự án (Thành công) -> 200
    @Test
    @DisplayName("[17.3] user-cv cập nhật tài liệu sau khi được gán role phù hợp (200)")
    void scenario173_UpdateDocumentWithProjectPermission_Success() throws Exception {
        Long docId = 1001L; // doc-proj-01
        DocumentResponse ok = new DocumentResponse();
        ok.setId(docId);
        ok.setTitle("Cập nhật nội dung");
        Mockito.when(documentService.updateDocumentMetadata(ArgumentMatchers.eq(docId), ArgumentMatchers.any(DocumentUpdateRequest.class)))
                .thenReturn(ok);

        String body = "{" +
                "\"title\":\"Cập nhật nội dung\"," +
                "\"description\":\"Mô tả sau khi có quyền\"" +
                "}";

        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/documents/{id}", docId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(docId));
    }
}
