package com.genifast.dms.service;

import java.time.Instant;
import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import com.genifast.dms.dto.request.DocumentCommentRequest;
import com.genifast.dms.dto.request.DocumentFilterRequest;
import com.genifast.dms.dto.request.DocumentShareRequest;
import com.genifast.dms.dto.request.DocumentUpdateRequest;
import com.genifast.dms.dto.request.SearchAndOrNotRequest;
import com.genifast.dms.dto.response.DocumentResponse;
import com.genifast.dms.dto.response.DocumentVersionResponse;

public interface DocumentService {
    DocumentResponse createDocument(String metadataJson, MultipartFile file);

    DocumentResponse getDocumentMetadata(Long id);

    ResponseEntity<Resource> downloadDocumentFile(Long id);

    DocumentResponse updateDocumentMetadata(Long id, DocumentUpdateRequest updateDto);

    void deleteDocument(Long id);

    Page<DocumentResponse> filterDocuments(DocumentFilterRequest filterDto, Pageable pageable);

    Page<DocumentResponse> searchDocuments(SearchAndOrNotRequest searchDto, Pageable pageable);

    DocumentResponse approveDocument(Long id);

    DocumentResponse rejectDocument(Long id, String reason); // Thêm lý do từ chối

    void shareDocument(Long id, DocumentShareRequest shareRequest);

    void trackDocumentHistory(Long id); // Để lấy lịch sử cho audit:log

    DocumentResponse submitDocument(Long id);

    void publishDocument(Long id);

    void archiveDocument(Long id);

    void signDocument(Long id);

    void lockDocument(Long id);

    void unlockDocument(Long id);

    void addComment(Long id, DocumentCommentRequest commentRequest);

    void restoreDocument(Long id);

    List<DocumentVersionResponse> getDocumentVersions(Long id);

    DocumentVersionResponse getSpecificDocumentVersion(Long id, Integer versionNumber);

    void notifyRecipients(Long id, String message); // Thêm message để gửi

    ResponseEntity<Resource> exportDocument(Long id, String format); // Thêm định dạng file export

    void forwardDocument(Long id, String recipientEmail);

    void distributeDocument(Long id, List<Long> departmentIds); // Phân phối tới nhiều phòng ban

    ResponseEntity<Resource> generateDocumentReport(String reportType, Long departmentId); // Báo cáo theo phòng ban

    String createShareLink(Long id, Instant expiryAt, boolean allowDownload);
}