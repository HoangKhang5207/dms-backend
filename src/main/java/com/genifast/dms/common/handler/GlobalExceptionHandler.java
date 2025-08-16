package com.genifast.dms.common.handler;

import com.genifast.dms.common.dto.ErrorResponseDTO;
import com.genifast.dms.common.exception.ApiException;
import com.genifast.dms.common.exception.StorageException;
import com.genifast.dms.common.exception.StorageFileNotFoundException;
import com.genifast.dms.common.utils.JwtUtils;
import com.genifast.dms.dto.request.AuditLogRequest;
import com.genifast.dms.entity.User;
import com.genifast.dms.repository.UserRepository;
import com.genifast.dms.service.AuditLogService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.catalina.connector.ClientAbortException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class GlobalExceptionHandler {

        private final AuditLogService auditLogService;
        private final UserRepository userRepository;

        /**
         * Bắt và xử lý các lỗi nghiệp vụ tùy chỉnh (kế thừa từ ApiException).
         */
        @ExceptionHandler(ApiException.class)
        public ResponseEntity<ErrorResponseDTO> handleApiException(ApiException ex, HttpServletRequest request) {
                // Nếu là lỗi ACCESS_DENIED từ nghiệp vụ, ghi log tương tự nhánh AuthorizationDenied
                try {
                        if (ex.getErrorCode() != null && ex.getErrorCode().name().equals("ACCESS_DENIED")) {
                                JwtUtils.getCurrentUserLogin().ifPresent(email -> {
                                        Optional<User> userOpt = userRepository.findByEmail(email);
                                        userOpt.ifPresent(user -> {
                                                String action = "ACCESS_DENIED";
                                                String details = String.format(
                                                                "Thất bại: User '%s' đã bị từ chối truy cập tài nguyên tại '%s'. Lý do: %s",
                                                                email, request.getRequestURI(), ex.getMessage());

                                                AuditLogRequest logRequest = AuditLogRequest.builder()
                                                                .action(action)
                                                                .details(details)
                                                                .userId(user.getId())
                                                                .build();
                                                auditLogService.logAction(logRequest);
                                        });
                                });
                        }
                } catch (Exception logEx) {
                        log.warn("Cannot write audit log for ApiException ACCESS_DENIED: {}", logEx.getMessage());
                }
                ErrorResponseDTO errorResponse = ErrorResponseDTO.builder()
                                .timestamp(LocalDateTime.now())
                                .status(ex.getErrorCode().getServiceCode())
                                .error(ex.getErrorCode().getHttpStatus().getReasonPhrase())
                                .message(ex.getMessage())
                                .path(request.getRequestURI())
                                .build();
                return new ResponseEntity<>(errorResponse, ex.getErrorCode().getHttpStatus());
        }

        /**
         * Bắt và xử lý các lỗi validation từ @Valid.
         */
        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ErrorResponseDTO> handleValidationExceptions(MethodArgumentNotValidException ex,
                        HttpServletRequest request) {
                Map<String, String> errors = new HashMap<>();
                ex.getBindingResult().getFieldErrors()
                                .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));

                ErrorResponseDTO errorResponse = ErrorResponseDTO.builder()
                                .timestamp(LocalDateTime.now())
                                .status(HttpStatus.BAD_REQUEST.value())
                                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                                .message("Validation failed for request body.")
                                .validationErrors(errors)
                                .path(request.getRequestURI())
                                .build();
                return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }

        @ExceptionHandler(BadCredentialsException.class)
        public ResponseEntity<ErrorResponseDTO> handleException(BadCredentialsException ex,
                        HttpServletRequest request) {
                ErrorResponseDTO errorResponse = ErrorResponseDTO.builder()
                                .timestamp(LocalDateTime.now())
                                .status(HttpStatus.BAD_REQUEST.value())
                                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                                .message("Invalid username or password")
                                .path(request.getRequestURI())
                                .build();
                return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }

        @ExceptionHandler(NoResourceFoundException.class)
        public ResponseEntity<ErrorResponseDTO> handleNoResourceFoundException(
                        NoResourceFoundException ex, HttpServletRequest request) {
                ErrorResponseDTO errorResponse = ErrorResponseDTO.builder()
                                .timestamp(LocalDateTime.now())
                                .status(HttpStatus.NOT_FOUND.value())
                                .error(HttpStatus.NOT_FOUND.getReasonPhrase())
                                .message("Resource not found")
                                .path(request.getRequestURI())
                                .build();
                return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
        }

        @ExceptionHandler(value = {
                        AuthorizationDeniedException.class,
                        AccessDeniedException.class
        })
        public ResponseEntity<ErrorResponseDTO> handleAuthorizationDeniedException(
                        Exception ex, HttpServletRequest request) {

                // --- LOGIC GHI LOG THẤT BẠI ---
                JwtUtils.getCurrentUserLogin().ifPresent(email -> {
                        Optional<User> userOpt = userRepository.findByEmail(email);
                        userOpt.ifPresent(user -> {
                                String action = "ACCESS_DENIED";
                                String details = String.format(
                                                "Thất bại: User '%s' đã cố gắng truy cập vào tài nguyên không được phép tại đường dẫn '%s'. Lý do: %s",
                                                email, request.getRequestURI(), ex.getMessage());

                                AuditLogRequest logRequest = AuditLogRequest.builder()
                                                .action(action)
                                                .details(details)
                                                .userId(user.getId()) // Chỉ lưu ID của user
                                                .build();

                                auditLogService.logAction(logRequest);
                        });
                });

                ErrorResponseDTO errorResponse = ErrorResponseDTO.builder()
                                .timestamp(LocalDateTime.now())
                                .status(HttpStatus.FORBIDDEN.value())
                                .error(HttpStatus.FORBIDDEN.getReasonPhrase())
                                .message("Bạn không có quyền truy cập hay thực hiện hành động này.")
                                .path(request.getRequestURI())
                                .build();
                return new ResponseEntity<>(errorResponse, HttpStatus.FORBIDDEN);
        }

        @ExceptionHandler(value = {
                        StorageException.class,
                        StorageFileNotFoundException.class
        })
        public ResponseEntity<ErrorResponseDTO> handleFileStorageException(
                        Exception ex, HttpServletRequest request) {
                ErrorResponseDTO errorResponse = ErrorResponseDTO.builder()
                                .timestamp(LocalDateTime.now())
                                .status(HttpStatus.BAD_REQUEST.value())
                                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                                .message("File upload failed")
                                .path(request.getRequestURI())
                                .build();
                return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }

        /**
         * Bắt tất cả các lỗi không xác định khác (lỗi 500).
         * Đây là một "catch-all" để đảm bảo không bao giờ trả về stack trace cho
         * client.
         */
        @ExceptionHandler(Exception.class)
        public ResponseEntity<ErrorResponseDTO> handleUncaughtException(Exception ex, HttpServletRequest request) {
                // Log lỗi chi tiết ở đây (quan trọng cho việc debug)
                log.error("An unexpected error occurred", ex);

                ErrorResponseDTO errorResponse = ErrorResponseDTO.builder()
                                .timestamp(LocalDateTime.now())
                                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                                .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                                .message("An unexpected internal server error occurred.")
                                .path(request.getRequestURI())
                                .build();
                return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        @ExceptionHandler(ClientAbortException.class)
        public void handleClientAbort(ClientAbortException ex) {
                // Không coi là lỗi nghiêm trọng, chỉ ghi debug nếu cần
                log.debug("Client aborted connection while streaming resource", ex);
        }
}