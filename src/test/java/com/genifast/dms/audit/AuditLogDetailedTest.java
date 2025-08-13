package com.genifast.dms.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.genifast.dms.common.handler.GlobalExceptionHandler;
import com.genifast.dms.dto.request.AuditLogRequest;
import com.genifast.dms.entity.AuditLog;
import com.genifast.dms.entity.Document;
import com.genifast.dms.entity.User;
import com.genifast.dms.mapper.AuditLogMapper;
import com.genifast.dms.repository.AuditLogRepository;
import com.genifast.dms.repository.DocumentRepository;
import com.genifast.dms.repository.UserRepository;
import com.genifast.dms.service.AuditLogService;
import com.genifast.dms.service.impl.AuditLogServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;

import jakarta.servlet.http.HttpServletRequest;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Audit Log - Ghi log chi tiết")
class AuditLogDetailedTest {

    // Mocks cho test "success" với AuditLogServiceImpl
    @Mock private AuditLogRepository auditLogRepository;
    @Mock private DocumentRepository documentRepository;
    @Mock private UserRepository userRepository;
    @Mock private AuditLogMapper auditLogMapper;
    @Mock private StringRedisTemplate stringRedisTemplate;
    @Mock private ObjectMapper objectMapper;
    @Mock private ListOperations<String, String> listOps;

    @Test
    @DisplayName("Success: flushAuditLogs lưu đầy đủ context vào DB")
    void successLogWithFullContext() throws Exception {
        // Arrange service với mocks
        when(stringRedisTemplate.opsForList()).thenReturn(listOps);
        AuditLogServiceImpl service = new AuditLogServiceImpl(
                auditLogRepository, documentRepository, userRepository, auditLogMapper, stringRedisTemplate, objectMapper);

        // Tiêm listOps (vì @PostConstruct không chạy trong unit test)
        Field f = AuditLogServiceImpl.class.getDeclaredField("listOps");
        f.setAccessible(true);
        f.set(service, listOps);

        // Dữ liệu vào hàng đợi Redis (JSON)
        String json = "{\"action\":\"READ_DOCUMENT\"}";
        when(listOps.range(eq("dms:audit_log_queue"), eq(-100L), eq(-1L))).thenReturn(List.of(json));

        // ObjectMapper deserialize thành DTO đầy đủ
        AuditLogRequest dto = AuditLogRequest.builder()
                .action("READ_DOCUMENT")
                .details("OK")
                .documentId(901L)
                .userId(401L)
                .delegatedByUserId(301L)
                .ipAddress("10.0.0.1")
                .sessionId("sess-1")
                .build();
        when(objectMapper.readValue(json, AuditLogRequest.class)).thenReturn(dto);

        // Tái tạo entity liên quan trong flush
        User user = User.builder().id(401L).email("u@org.vn").build();
        User delegatedBy = User.builder().id(301L).email("a@org.vn").build();
        Document doc = Document.builder().id(901L).title("Doc").build();
        when(userRepository.findById(401L)).thenReturn(Optional.of(user));
        when(userRepository.findById(301L)).thenReturn(Optional.of(delegatedBy));
        when(documentRepository.findById(901L)).thenReturn(Optional.of(doc));

        // Act
        service.flushAuditLogs();

        // Assert
        ArgumentCaptor<List<AuditLog>> captor = ArgumentCaptor.forClass(List.class);
        verify(auditLogRepository).saveAll(captor.capture());
        List<AuditLog> saved = captor.getValue();
        assertEquals(1, saved.size());
        AuditLog log = saved.get(0);
        assertEquals("READ_DOCUMENT", log.getAction());
        assertEquals("OK", log.getDetails());
        assertEquals("10.0.0.1", log.getIpAddress());
        assertEquals("sess-1", log.getSessionId());
        assertEquals(401L, log.getUser().getId());
        assertEquals(301L, log.getDelegatedBy().getId());
        assertEquals(901L, log.getDocument().getId());

        // Đồng thời trim hàng đợi
        verify(stringRedisTemplate, atLeastOnce()).opsForList();
        verify(stringRedisTemplate.opsForList()).trim("dms:audit_log_queue", 0, -101);
    }

    @Test
    @DisplayName("Failure: AuthorizationDenied -> GlobalExceptionHandler ghi ACCESS_DENIED với context")
    void failureAuthorizationDeniedLogged() {
        // Arrange
        AuditLogService auditLogService = mock(AuditLogService.class);
        UserRepository userRepo = mock(UserRepository.class);
        GlobalExceptionHandler advice = new GlobalExceptionHandler(auditLogService, userRepo);

        // SecurityContext có principal là email
        String email = "user@test.vn";
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(email, "pwd"));

        // User tồn tại
        User user = User.builder().id(777L).email(email).build();
        when(userRepo.findByEmail(email)).thenReturn(Optional.of(user));

        // Request mock
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/documents/42");

        // Act
        advice.handleAuthorizationDeniedException(new AuthorizationDeniedException("Forbidden by policy"), request);

        // Assert: logAction được gọi với ACCESS_DENIED và chứa context
        ArgumentCaptor<AuditLogRequest> reqCap = ArgumentCaptor.forClass(AuditLogRequest.class);
        verify(auditLogService).logAction(reqCap.capture());
        AuditLogRequest logged = reqCap.getValue();
        assertEquals("ACCESS_DENIED", logged.getAction());
        assertEquals(777L, logged.getUserId());
        assertTrue(logged.getDetails().contains(email));
        assertTrue(logged.getDetails().contains("/api/documents/42"));
    }
}
