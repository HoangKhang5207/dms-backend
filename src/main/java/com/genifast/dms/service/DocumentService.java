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

    DocumentResponse getDocumentMetadata(Long docId);

    ResponseEntity<Resource> downloadDocumentFile(Long docId);

    DocumentResponse updateDocumentMetadata(Long docId, DocumentUpdateRequest updateDto);

    void deleteDocument(Long docId);

    Page<DocumentResponse> filterDocuments(DocumentFilterRequest filterDto, Pageable pageable);

    Page<DocumentResponse> searchDocuments(SearchAndOrNotRequest searchDto, Pageable pageable);

    DocumentResponse approveDocument(Long docId);

    DocumentResponse rejectDocument(Long docId, String reason); // Thêm lý do từ chối

    void shareDocument(Long docId, DocumentShareRequest shareRequest);

    void trackDocumentHistory(Long docId); // Để lấy lịch sử cho audit:log

    DocumentResponse submitDocument(Long docId);

    DocumentResponse publishDocument(Long docId);

    DocumentResponse archiveDocument(Long docId);

    DocumentResponse signDocument(Long docId);

    DocumentResponse lockDocument(Long docId);

    DocumentResponse unlockDocument(Long docId);

    void addComment(Long docId, DocumentCommentRequest commentRequest);

    DocumentResponse restoreDocument(Long docId);

    List<DocumentVersionResponse> getDocumentVersions(Long docId);

    DocumentVersionResponse getSpecificDocumentVersion(Long docId, Integer versionNumber);

    void notifyRecipients(Long docId, String message); // Thêm message để gửi

    ResponseEntity<Resource> exportDocument(Long docId, String format); // Thêm định dạng file export

    void forwardDocument(Long docId, String recipientEmail);

    void distributeDocument(Long docId, List<Long> departmentIds); // Phân phối tới nhiều phòng ban

    ResponseEntity<Resource> generateDocumentReport(String reportType, Long departmentId); // Báo cáo theo phòng ban

    String createShareLink(Long docId, Instant expiryAt, boolean allowDownload);
}