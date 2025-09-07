package com.genifast.dms.controller;

import com.genifast.dms.common.exception.ApiException;
import com.genifast.dms.common.constant.ErrorCode;
import com.genifast.dms.entity.Document;
import com.genifast.dms.repository.DocumentRepository;
import com.genifast.dms.entity.DocumentVersion;
import com.genifast.dms.repository.DocumentVersionRepository;
import com.genifast.dms.service.FileStorageService;
import com.genifast.dms.service.AuditLogService;
import com.genifast.dms.dto.request.AuditLogRequest;
import com.genifast.dms.common.utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
public class PublicDocumentController {

    private final DocumentRepository documentRepository;
    private final DocumentVersionRepository documentVersionRepository;
    private final FileStorageService fileStorageService;
    private final AuditLogService auditLogService;

    // Giữ endpoint cũ để tương thích ngược
    @RequestMapping("/api/public/documents")
    @GetMapping("/share/{token}")
    public ResponseEntity<ByteArrayResource> getSharedDocument(@PathVariable String token) throws Exception {
        // 1. Tìm tài liệu bằng shareToken
        Document document = documentRepository.findByShareToken(token)
                .orElseThrow(() -> new ApiException(ErrorCode.DOCUMENT_NOT_FOUND,
                        "Liên kết chia sẻ không hợp lệ hoặc đã bị xóa."));

        // 2. Kiểm tra thời gian hết hạn
        if (document.getPublicShareExpiryAt() != null && document.getPublicShareExpiryAt().isBefore(Instant.now())) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "Liên kết chia sẻ đã hết hạn.");
        }

        // 3. Kiểm tra quyền tải xuống
        if (!document.isAllowPublicDownload()) {
            throw new ApiException(ErrorCode.USER_NO_PERMISSION, "Bạn không có quyền tải xuống tài liệu này.");
        }

        // 4. Lấy file với watermark cho Visitor
        byte[] data = fileStorageService.retrieveFileForVisitor(document);

        ByteArrayResource resource = new ByteArrayResource(data);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + document.getOriginalFilename() + "\"")
                .contentType(MediaType.parseMediaType("application/" + document.getType()))
                .body(resource);
    }

    // [21.4] Xem phiên bản cụ thể của tài liệu PUBLIC sau khi trả phí
    @GetMapping("/api/v1/documents/public/{token}/versions/{version}")
    public ResponseEntity<ByteArrayResource> viewPublicDocumentVersion(
            @PathVariable("token") String token,
            @PathVariable("version") Integer version,
            @RequestHeader(value = "X-Payment-Status", required = false) String paymentStatus) throws Exception {
        Document document = validateAndGetByToken(token);

        boolean ok = paymentStatus != null
                && (paymentStatus.equalsIgnoreCase("paid") || paymentStatus.equalsIgnoreCase("purchased"));
        if (!ok) {
            logAudit("READ_VERSION_FAILED",
                    String.format("Truy cập phiên bản %s fail cho tài liệu ID %s: chưa thanh toán.",
                            String.valueOf(version), document.getId()),
                    document.getId());
            throw new ApiException(ErrorCode.ACCESS_DENIED, "Payment required for full access.");
        }

        byte[] data;
        // Ưu tiên nội dung theo version từ bảng document_versions nếu có
        DocumentVersion dv = null;
        if (documentVersionRepository != null) {
            dv = documentVersionRepository
                    .findByDocumentIdAndVersionNumber(document.getId(), version)
                    .orElse(null);
        }
        if (dv != null && dv.getContent() != null && !dv.getContent().isBlank()) {
            data = dv.getContent().getBytes();
        } else {
            data = fileStorageService.retrieveFileForVisitor(document);
        }
        ByteArrayResource resource = new ByteArrayResource(data);

        logAudit("READ_VERSION", String.format("Xem phiên bản %s của tài liệu công khai ID %s sau thanh toán.",
                String.valueOf(version), document.getId()), document.getId());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + document.getOriginalFilename() + "\"")
                .contentType(MediaType.parseMediaType("application/" + document.getType()))
                .body(resource);
    }

    // --- SCENARIO 7: Visitor endpoints ---

    private void logAudit(String action, String details, Long documentId) {
        // Visitor có thể không đăng nhập, cố gắng lấy userId nếu có; nếu không để null
        Optional<String> emailOpt = JwtUtils.getCurrentUserLogin();
        Long userId = null;
        if (emailOpt.isPresent()) {
            // Không có UserRepository ở đây; để userId null, AuditLogServiceImpl sẽ bỏ qua
            // khi flush
        }
        AuditLogRequest req = AuditLogRequest.builder()
                .action(action)
                .details(details)
                .documentId(documentId)
                .userId(userId)
                .build();
        try {
            auditLogService.logAction(req);
        } catch (Exception ignored) {
        }
    }

    private Document validateAndGetByToken(String token) {
        Document document = documentRepository.findByShareToken(token)
                .orElse(null);
        if (document == null) {
            logAudit("READ_DOCUMENT_PUBLIC_FAILED",
                    String.format("Token '%s' không hợp lệ hoặc không được phép.", token), null);
            throw new ApiException(ErrorCode.ACCESS_DENIED, "Invalid or unauthorized public link.");
        }

        if (document.getPublicShareExpiryAt() != null && document.getPublicShareExpiryAt().isBefore(Instant.now())) {
            logAudit("READ_DOCUMENT_PUBLIC_FAILED",
                    String.format("Liên kết công khai cho tài liệu ID %s đã hết hạn.", document.getId()),
                    document.getId());
            throw new ApiException(ErrorCode.ACCESS_DENIED, "Public link expired.");
        }
        return document;
    }

    // [20.1] Khởi tạo thanh toán VNPay (trả về URL sandbox)
    @PostMapping("/api/v1/documents/public/{token}/initiate-payment")
    public ResponseEntity<Map<String, String>> initiatePayment(@PathVariable("token") String token) {
        Document document = validateAndGetByToken(token);

        String paymentUrl = "https://sandbox.vnpay.vn/paymentv2/vpcpay.html";

        logAudit("INITIATE_PAYMENT",
                String.format("Initiated payment for document ID %s via token %s", document.getId(), token),
                document.getId());

        return ResponseEntity.ok(Map.of("paymentUrl", paymentUrl));
    }

    // [7.1] Preview tài liệu PUBLIC (một phần, watermark)
    @GetMapping("/api/v1/documents/public/{token}/preview")
    public ResponseEntity<ByteArrayResource> previewPublicDocument(@PathVariable("token") String token)
            throws Exception {
        Document document = validateAndGetByToken(token);

        byte[] data = fileStorageService.retrieveFileForVisitor(document);
        ByteArrayResource resource = new ByteArrayResource(data);

        logAudit("PREVIEW_DOCUMENT_PUBLIC", String.format("Preview công khai tài liệu ID %s.", document.getId()),
                document.getId());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + document.getOriginalFilename() + "\"")
                .contentType(MediaType.parseMediaType("application/" + document.getType()))
                .body(resource);
    }

    // [7.2] Xem toàn bộ tài liệu PUBLIC sau khi trả phí
    @GetMapping("/api/v1/documents/public/{token}")
    public ResponseEntity<ByteArrayResource> viewPublicDocument(
            @PathVariable("token") String token,
            @RequestHeader(value = "X-Payment-Status", required = false) String paymentStatus) throws Exception {
        Document document = validateAndGetByToken(token);
        // Chấp nhận cả 'paid' và 'purchased' để tương thích kịch bản
        boolean ok = paymentStatus != null
                && (paymentStatus.equalsIgnoreCase("paid") || paymentStatus.equalsIgnoreCase("purchased"));
        if (!ok) {
            logAudit("READ_DOCUMENT_PUBLIC_FAILED",
                    String.format("Truy cập toàn bộ fail cho tài liệu ID %s: chưa thanh toán.", document.getId()),
                    document.getId());
            throw new ApiException(ErrorCode.ACCESS_DENIED, "Payment required for full access.");
        }

        byte[] data = fileStorageService.retrieveFileForVisitor(document);
        ByteArrayResource resource = new ByteArrayResource(data);

        logAudit("READ_DOCUMENT_PUBLIC",
                String.format("Xem toàn bộ tài liệu công khai ID %s sau thanh toán.", document.getId()),
                document.getId());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + document.getOriginalFilename() + "\"")
                .contentType(MediaType.parseMediaType("application/" + document.getType()))
                .body(resource);
    }

    // [7.3] Tải tài liệu PUBLIC sau khi trả phí
    @GetMapping("/api/v1/documents/public/{token}/download")
    public ResponseEntity<ByteArrayResource> downloadPublicDocument(
            @PathVariable("token") String token,
            @RequestHeader(value = "X-Payment-Status", required = false) String paymentStatus) throws Exception {
        Document document = validateAndGetByToken(token);

        if (paymentStatus == null || !paymentStatus.equalsIgnoreCase("paid_download")) {
            logAudit("DOWNLOAD_DOCUMENT_PUBLIC_FAILED",
                    String.format("Tải xuống fail cho tài liệu ID %s: chưa thanh toán.", document.getId()),
                    document.getId());
            throw new ApiException(ErrorCode.ACCESS_DENIED, "Payment required for download.");
        }

        if (!document.isAllowPublicDownload()) {
            logAudit("DOWNLOAD_DOCUMENT_PUBLIC_FAILED",
                    String.format("Tải xuống fail cho tài liệu ID %s: không được phép.", document.getId()),
                    document.getId());
            throw new ApiException(ErrorCode.ACCESS_DENIED, "Public download not allowed.");
        }

        byte[] data = fileStorageService.retrieveFileForVisitor(document);
        ByteArrayResource resource = new ByteArrayResource(data);

        logAudit("DOWNLOAD_DOCUMENT_PUBLIC",
                String.format("Tải xuống tài liệu công khai ID %s sau thanh toán.", document.getId()),
                document.getId());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + document.getOriginalFilename() + "\"")
                .contentType(MediaType.parseMediaType("application/" + document.getType()))
                .body(resource);
    }
}