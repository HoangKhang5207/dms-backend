package com.genifast.dms.service;

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

    void approveDocument(Long docId);

    void rejectDocument(Long docId);

    void shareDocument(Long docId, DocumentShareRequest shareRequest);

    void trackDocumentHistory(Long docId); // Để lấy lịch sử cho audit:log

    void submitDocument(Long docId);

    void publishDocument(Long docId);

    void archiveDocument(Long docId);

    void signDocument(Long docId);

    void lockDocument(Long docId);

    void unlockDocument(Long docId);

    void addComment(Long docId, DocumentCommentRequest commentRequest);

    void restoreDocument(Long docId);

    List<DocumentVersionResponse> getDocumentVersions(Long docId);

    DocumentVersionResponse getSpecificDocumentVersion(Long docId, Integer versionNumber);

    void notifyRecipients(Long docId);

    ResponseEntity<Resource> exportDocument(Long docId);

    void forwardDocument(Long docId, String recipientEmail);

    void distributeDocument(Long docId, Long departmentId);

    ResponseEntity<Resource> generateDocumentReport(String reportType);
}