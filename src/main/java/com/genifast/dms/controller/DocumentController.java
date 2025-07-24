package com.genifast.dms.controller;

import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.genifast.dms.dto.request.DocumentFilterRequest;
import com.genifast.dms.dto.request.DocumentUpdateRequest;
import com.genifast.dms.dto.request.SearchAndOrNotRequest;
import com.genifast.dms.dto.response.DocumentResponse;
import com.genifast.dms.service.DocumentService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping(consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    public ResponseEntity<DocumentResponse> createDocument(
            @RequestPart("metadata") String metadataJson,
            @RequestPart("file") MultipartFile file) {

        DocumentResponse createdDocument = documentService.createDocument(metadataJson, file);
        return new ResponseEntity<>(createdDocument, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DocumentResponse> getDocumentMetadata(@PathVariable Long id) {
        return ResponseEntity.ok(documentService.getDocumentMetadata(id));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long id) {
        return documentService.downloadDocumentFile(id);
    }

    @PutMapping("/{id}")
    public ResponseEntity<DocumentResponse> updateDocumentMetadata(@PathVariable Long id,
            @Valid @RequestBody DocumentUpdateRequest updateDto) {
        return ResponseEntity.ok(documentService.updateDocumentMetadata(id, updateDto));
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
}
