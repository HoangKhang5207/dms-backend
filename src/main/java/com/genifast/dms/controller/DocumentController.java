package com.genifast.dms.controller;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.genifast.dms.dto.request.DocumentCommentRequest;
import com.genifast.dms.dto.request.DocumentFilterRequest;
import com.genifast.dms.dto.request.DocumentShareRequest;
import com.genifast.dms.dto.request.DocumentUpdateRequest;
import com.genifast.dms.dto.request.SearchAndOrNotRequest;
import com.genifast.dms.dto.response.DocumentResponse;
import com.genifast.dms.dto.response.DocumentVersionResponse;
import com.genifast.dms.entity.Document;
import com.genifast.dms.service.DocumentService;
import com.genifast.dms.service.FileStorageService;
import com.genifast.dms.service.util.WatermarkService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;
    private final FileStorageService fileStorageService;
    private final WatermarkService watermarkService;

    // @PostMapping(consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    // public ResponseEntity<DocumentResponse> createDocument(
    // @RequestPart("metadata") String metadataJson,
    // @RequestPart("file") MultipartFile file) {

    // DocumentResponse createdDocument =
    // documentService.createDocument(metadataJson, file);
    // return new ResponseEntity<>(createdDocument, HttpStatus.CREATED);
    // }

    @PostMapping(value = "/upload-multiple", consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    public ResponseEntity<List<Document>> uploadMultipleFiles(
            @RequestPart("files") MultipartFile[] files,
            @RequestParam(required = false) String password) throws Exception {
        List<Document> uploadedDocuments = fileStorageService.storeMultipleFiles(files, password);
        return new ResponseEntity<>(uploadedDocuments, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DocumentResponse> getDocumentMetadata(@PathVariable Long id) {
        return ResponseEntity.ok(documentService.getDocumentMetadata(id));
    }

    // @GetMapping("/{id}/download")
    // public ResponseEntity<Resource> downloadFile(@PathVariable Long id) {
    // return documentService.downloadDocumentFile(id);
    // }

    @GetMapping("/{fileId}/retrieve")
    public ResponseEntity<ByteArrayResource> retrieveFile(
            @PathVariable String fileId,
            @RequestParam(required = false) String password,
            @RequestParam(defaultValue = "true") boolean withWatermark) throws Exception {
        byte[] data = fileStorageService.retrieveFileById(fileId, password, withWatermark);
        String filename = fileStorageService.getOriginalFileName(fileId);
        String contentType = fileStorageService.getOriginalDocumentType(fileId);

        ByteArrayResource resource = new ByteArrayResource(data);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("application/" + contentType))
                .body(resource);
    }

    @PutMapping("/{id}")
    public ResponseEntity<DocumentResponse> updateDocumentMetadata(@PathVariable Long id,
            @Valid @RequestBody DocumentUpdateRequest updateDto) {
        return ResponseEntity.ok(documentService.updateDocumentMetadata(id, updateDto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDocument(@PathVariable("id") Long docId) {
        documentService.deleteDocument(docId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/filter")
    public ResponseEntity<Page<DocumentResponse>> filterDocuments(
            @Valid DocumentFilterRequest filterDto,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(documentService.filterDocuments(filterDto, pageable));
    }

    @PostMapping("/search")
    public ResponseEntity<Page<DocumentResponse>> searchDocuments(
            @Valid @RequestBody SearchAndOrNotRequest searchDto,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(documentService.searchDocuments(searchDto, pageable));
    }

    @PostMapping("/download-multiple-zip")
    public ResponseEntity<ByteArrayResource> downloadMultipleFilesAsZip(
            @RequestBody List<Map<String, String>> fileDetails) throws Exception {
        byte[] zipData = fileStorageService.downloadMultipleFilesAsZip(fileDetails);
        ByteArrayResource resource = new ByteArrayResource(zipData);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"documents.zip\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    @PutMapping("/{fileId}/change-password")
    public ResponseEntity<Void> changeFilePassword(
            @PathVariable String fileId,
            @RequestParam String oldPassword,
            @RequestParam String newPassword) throws Exception {
        fileStorageService.changeFilePassword(fileId, oldPassword, newPassword);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/all")
    public ResponseEntity<List<Document>> getAllDocuments() {
        List<Document> documents = fileStorageService.getAllDocuments();
        return ResponseEntity.ok(documents);
    }

    @DeleteMapping("/{fileId}")
    public ResponseEntity<Void> deleteFile(@PathVariable String fileId) throws Exception {
        fileStorageService.deleteFileById(fileId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/check-local-watermark")
    public ResponseEntity<Map<String, Object>> checkLocalWatermark(@RequestParam("file") MultipartFile file) {
        try {
            String fileName = file.getOriginalFilename();
            List<String> foundWatermarks = new ArrayList<>();
            boolean hasWatermark = false;

            if (fileName != null) {
                if (fileName.toLowerCase().endsWith(".pdf")) {
                    foundWatermarks = watermarkService.extractPossibleWatermarkIdsFromPdf(file.getInputStream());
                } else if (fileName.toLowerCase().endsWith(".docx")) {
                    foundWatermarks = watermarkService.extractHiddenStringsFromDocx(file.getInputStream());
                }

                // Remove duplicates from foundWatermarks at the backend
                foundWatermarks = new ArrayList<>(new HashSet<>(foundWatermarks));

                // Check if any of the found strings are valid UUIDs
                for (String wm : foundWatermarks) {
                    if (isValidUUID(wm)) {
                        hasWatermark = true;
                        break;
                    }
                }
            }

            Map<String, Object> response = Map.of("hasWatermark", hasWatermark, "foundWatermarks", foundWatermarks);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("hasWatermark", false, "foundWatermarks", new ArrayList<>()));
        }
    }

    // Helper method to validate UUID format
    private boolean isValidUUID(String uuid) {
        try {
            UUID.fromString(uuid);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    @PostMapping("/validate-password")
    public ResponseEntity<Boolean> validatePassword(@RequestBody Map<String, String> payload) {
        String fileId = payload.get("fileId");
        String password = payload.get("password");
        try {
            boolean isValid = fileStorageService.validateFilePassword(fileId, password);
            return ResponseEntity.ok(isValid);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(false);
        }
    }

    @DeleteMapping("/delete-multiple")
    public ResponseEntity<Void> deleteMultipleFiles(@RequestBody List<String> fileIds) throws Exception {
        fileStorageService.deleteMultipleFilesByIds(fileIds);
        return ResponseEntity.noContent().build();
    }

    // Bổ sung các endpoint giả định cho DMS - các hành động quản lý tài liệu
    @PostMapping("/{id}/approve")
    public ResponseEntity<String> approveDocument(@PathVariable("id") Long docId) {
        documentService.approveDocument(docId);
        return ResponseEntity.ok("Tài liệu đã được phê duyệt thành công.");
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<String> rejectDocument(@PathVariable("id") Long docId) {
        documentService.rejectDocument(docId);
        return ResponseEntity.ok("Tài liệu đã bị từ chối.");
    }

    @PostMapping("/{id}/share")
    public ResponseEntity<String> shareDocument(@PathVariable("id") Long docId,
            @Valid @RequestBody DocumentShareRequest shareRequest) {
        documentService.shareDocument(docId, shareRequest);
        return ResponseEntity.ok("Tài liệu đã được chia sẻ thành công.");
    }

    @GetMapping("/{id}/track")
    public ResponseEntity<String> trackDocument(@PathVariable("id") Long docId) {
        documentService.trackDocumentHistory(docId);
        // Trong thực tế, endpoint này sẽ gọi AuditLogController để lấy dữ liệu
        return ResponseEntity.ok("Hành động theo dõi đã được ghi lại. Xem chi tiết trong Audit Logs.");
    }

    @PostMapping("/{id}/submit")
    public ResponseEntity<String> submitDocument(@PathVariable("id") Long docId) {
        documentService.submitDocument(docId);
        return ResponseEntity.ok("Tài liệu đã được trình ký thành công.");
    }

    @PostMapping("/{id}/publish")
    public ResponseEntity<String> publishDocument(@PathVariable("id") Long docId) {
        documentService.publishDocument(docId);
        return ResponseEntity.ok("Tài liệu đã được công khai thành công.");
    }

    @PostMapping("/{id}/archive")
    public ResponseEntity<String> archiveDocument(@PathVariable("id") Long docId) {
        documentService.archiveDocument(docId);
        return ResponseEntity.ok("Tài liệu đã được lưu trữ thành công.");
    }

    @PostMapping("/{id}/sign")
    public ResponseEntity<String> signDocument(@PathVariable("id") Long docId) {
        documentService.signDocument(docId);
        return ResponseEntity.ok("Tài liệu đã được ký thành công.");
    }

    @PostMapping("/{id}/lock")
    public ResponseEntity<String> lockDocument(@PathVariable("id") Long docId) {
        documentService.lockDocument(docId);
        return ResponseEntity.ok("Tài liệu đã được khóa.");
    }

    @PostMapping("/{id}/unlock")
    public ResponseEntity<String> unlockDocument(@PathVariable("id") Long docId) {
        documentService.unlockDocument(docId);
        return ResponseEntity.ok("Tài liệu đã được mở khóa.");
    }

    @PostMapping("/{id}/comments")
    public ResponseEntity<String> addComment(@PathVariable("id") Long docId,
            @Valid @RequestBody DocumentCommentRequest commentRequest) {
        documentService.addComment(docId, commentRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body("Bình luận đã được thêm.");
    }

    @PostMapping("/{id}/restore")
    public ResponseEntity<String> restoreDocument(@PathVariable("id") Long docId) {
        documentService.restoreDocument(docId);
        return ResponseEntity.ok("Tài liệu đã được khôi phục.");
    }

    @GetMapping("/{id}/versions")
    public ResponseEntity<List<DocumentVersionResponse>> getDocumentVersions(@PathVariable("id") Long docId) {
        return ResponseEntity.ok(documentService.getDocumentVersions(docId));
    }

    @GetMapping("/{id}/versions/{versionNumber}")
    public ResponseEntity<DocumentVersionResponse> getSpecificDocumentVersion(@PathVariable("id") Long docId,
            @PathVariable Integer versionNumber) {
        return ResponseEntity.ok(documentService.getSpecificDocumentVersion(docId, versionNumber));
    }

    @PostMapping("/{id}/notify")
    public ResponseEntity<String> notifyRecipients(@PathVariable("id") Long docId) {
        documentService.notifyRecipients(docId);
        return ResponseEntity.ok("Đã gửi thông báo tới những người liên quan.");
    }

    @GetMapping("/{id}/export")
    public ResponseEntity<Resource> exportDocument(@PathVariable("id") Long docId) {
        return documentService.exportDocument(docId);
    }

    @PostMapping("/{id}/forward")
    public ResponseEntity<String> forwardDocument(@PathVariable("id") Long docId, @RequestParam String recipientEmail) {
        documentService.forwardDocument(docId, recipientEmail);
        return ResponseEntity.ok("Tài liệu đã được chuyển tiếp thành công.");
    }

    @PostMapping("/{id}/distribute")
    public ResponseEntity<String> distributeDocument(@PathVariable("id") Long docId, @RequestParam Long departmentId) {
        documentService.distributeDocument(docId, departmentId);
        return ResponseEntity.ok("Tài liệu đã được phân phối.");
    }

    @GetMapping("/report")
    public ResponseEntity<Resource> generateReport(@RequestParam String reportType) {
        // reportType có thể là 'monthly_summary', 'user_activity', etc.
        return documentService.generateDocumentReport(reportType);
    }
}
