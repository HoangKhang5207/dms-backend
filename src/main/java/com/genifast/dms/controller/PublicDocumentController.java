package com.genifast.dms.controller;

import com.genifast.dms.common.exception.ApiException;
import com.genifast.dms.common.constant.ErrorCode;
import com.genifast.dms.entity.Document;
import com.genifast.dms.repository.DocumentRepository;
import com.genifast.dms.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;

@RestController
@RequestMapping("/api/public/documents")
@RequiredArgsConstructor
public class PublicDocumentController {

    private final DocumentRepository documentRepository;
    private final FileStorageService fileStorageService;

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
}