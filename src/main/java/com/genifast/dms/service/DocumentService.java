package com.genifast.dms.service;

import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import com.genifast.dms.dto.request.DocumentFilterRequest;
import com.genifast.dms.dto.request.DocumentUpdateRequest;
import com.genifast.dms.dto.request.SearchAndOrNotRequest;
import com.genifast.dms.dto.response.DocumentResponse;

public interface DocumentService {
    DocumentResponse createDocument(String metadataJson, MultipartFile file);

    DocumentResponse getDocumentMetadata(Long docId);

    ResponseEntity<Resource> downloadDocumentFile(Long docId);

    DocumentResponse updateDocumentMetadata(Long docId, DocumentUpdateRequest updateDto);

    Page<DocumentResponse> filterDocuments(DocumentFilterRequest filterDto, Pageable pageable);

    Page<DocumentResponse> searchDocuments(SearchAndOrNotRequest searchDto, Pageable pageable);
}