package com.genifast.dms.service.impl;

import java.io.IOException;

import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.genifast.dms.common.constant.ErrorCode;
import com.genifast.dms.common.constant.ErrorMessage;
import com.genifast.dms.common.constant.FileSizeFormatter;
import com.genifast.dms.common.exception.ApiException;
import com.genifast.dms.common.utils.JwtUtils;
import com.genifast.dms.dto.request.DocumentCreateRequest;
import com.genifast.dms.dto.request.DocumentFilterRequest;
import com.genifast.dms.dto.request.DocumentUpdateRequest;
import com.genifast.dms.dto.request.SearchAndOrNotRequest;
import com.genifast.dms.dto.response.DocumentResponse;
import com.genifast.dms.entity.Category;
import com.genifast.dms.entity.Document;
import com.genifast.dms.entity.User;
import com.genifast.dms.mapper.DocumentMapper;
import com.genifast.dms.repository.CategoryRepository;
import com.genifast.dms.repository.DocumentRepository;
import com.genifast.dms.repository.PrivateDocumentRepository;
import com.genifast.dms.repository.UserRepository;
import com.genifast.dms.repository.specifications.DocumentSpecification;
import com.genifast.dms.service.DocumentService;
import com.genifast.dms.service.FileStorageService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentServiceImpl implements DocumentService {
    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final PrivateDocumentRepository privateDocumentRepository;
    private final FileStorageService fileStorageService;
    private final DocumentMapper documentMapper;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public DocumentResponse createDocument(String metadataJson, MultipartFile file) {
        User currentUser = findUserByEmail(JwtUtils.getCurrentUserLogin().orElse(""));

        DocumentCreateRequest createDto;
        try {
            createDto = objectMapper.readValue(metadataJson, DocumentCreateRequest.class);
        } catch (IOException e) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "Invalid metadata format.");
        }

        Category category = categoryRepository.findById(createDto.getCategoryId())
                .orElseThrow(() -> new ApiException(ErrorCode.CATEGORY_NOT_FOUND,
                        ErrorMessage.CATEGORY_NOT_FOUND.getMessage()));

        authorizeUserIsMemberOfOrg(currentUser, category.getOrganization().getId());

        String filePath = fileStorageService.store(file);

        Document doc = Document.builder()
                .title(createDto.getTitle())
                .content(createDto.getContent())
                .description(createDto.getDescription())
                .type(StringUtils.getFilenameExtension(filePath))
                .category(category)
                .department(category.getDepartment())
                .organization(category.getOrganization())
                .status(1) // Active
                .accessType(createDto.getAccessType())
                .filePath(filePath)
                .fileId(category.getDepartment().getName() + "/" + category.getName())
                .storageCapacity(file.getSize())
                .storageUnit(FileSizeFormatter.format(file.getSize()))
                .originalFilename(file.getOriginalFilename())
                .contentType(file.getContentType())
                .build();

        Document savedDoc = documentRepository.save(doc);
        log.info("Document '{}' (ID: {}) uploaded by {}", savedDoc.getTitle(), savedDoc.getId(),
                currentUser.getEmail());
        return documentMapper.toDocumentResponse(savedDoc);
    }

    @Override
    public DocumentResponse getDocumentMetadata(Long docId) {
        User currentUser = findUserByEmail(JwtUtils.getCurrentUserLogin().orElse(""));
        Document document = findDocById(docId);
        authorizeUserCanAccessDocument(currentUser, document);
        return documentMapper.toDocumentResponse(document);
    }

    @Override
    public ResponseEntity<Resource> downloadDocumentFile(Long docId) {
        User currentUser = findUserByEmail(JwtUtils.getCurrentUserLogin().orElse(""));
        Document document = findDocById(docId);
        authorizeUserCanAccessDocument(currentUser, document);

        Resource resource = fileStorageService.loadAsResource(document.getFilePath());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(document.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + document.getOriginalFilename() + "\"")
                .body(resource);
    }

    @Override
    @Transactional
    public DocumentResponse updateDocumentMetadata(Long docId, DocumentUpdateRequest updateDto) {
        User currentUser = findUserByEmail(JwtUtils.getCurrentUserLogin().orElse(""));
        Document document = findDocById(docId);
        authorizeUserCanEditDocument(currentUser, document);

        document.setTitle(updateDto.getTitle());
        document.setDescription(updateDto.getDescription());
        if (updateDto.getCategoryId() != null && !updateDto.getCategoryId().equals(document.getCategory().getId())) {
            Category newCategory = categoryRepository.findById(updateDto.getCategoryId())
                    .orElseThrow(() -> new ApiException(ErrorCode.CATEGORY_NOT_FOUND,
                            ErrorMessage.CATEGORY_NOT_FOUND.getMessage()));
            document.setCategory(newCategory);
        }

        Document updatedDoc = documentRepository.save(document);
        log.info("Document ID {} metadata updated by {}", docId, currentUser.getEmail());
        return documentMapper.toDocumentResponse(updatedDoc);
    }

    @Override
    public Page<DocumentResponse> filterDocuments(DocumentFilterRequest filterDto, Pageable pageable) {
        User currentUser = findUserByEmail(JwtUtils.getCurrentUserLogin().orElse(""));

        // Bắt đầu bằng Specification kiểm tra quyền truy cập
        Specification<Document> spec = DocumentSpecification.hasAccess(currentUser);

        // Nối thêm các điều kiện lọc
        spec = spec.and(DocumentSpecification.titleContains(filterDto.getTitle()));
        spec = spec.and(DocumentSpecification.hasType(filterDto.getType()));
        spec = spec.and(DocumentSpecification.hasCreatedBy(filterDto.getCreatedBy()));
        spec = spec.and(
                DocumentSpecification.createdBetween(filterDto.getCreatedFromDate(), filterDto.getCreatedToDate()));

        Page<Document> documentPage = documentRepository.findAll(spec, pageable);

        return documentPage.map(documentMapper::toDocumentResponse);
    }

    @Override
    public Page<DocumentResponse> searchDocuments(SearchAndOrNotRequest searchDto, Pageable pageable) {
        User currentUser = findUserByEmail(JwtUtils.getCurrentUserLogin().orElse(""));

        // Kết hợp Specification quyền truy cập và Specification tìm kiếm
        Specification<Document> spec = DocumentSpecification.hasAccess(currentUser)
                .and(DocumentSpecification.matchesAndOrNot(searchDto));

        Page<Document> documentPage = documentRepository.findAll(spec, pageable);

        return documentPage.map(documentMapper::toDocumentResponse);
    }

    // ------ Helper Method ------
    private void authorizeUserCanAccessDocument(User user, Document document) {
        switch (document.getAccessType()) {
            case 1: // Public
                return;
            case 2: // Organization
                if (user.getOrganization() != null
                        && user.getOrganization().getId().equals(document.getOrganization().getId()))
                    return;
                break;
            case 3: // Department
                if (user.getDepartment() != null
                        && user.getDepartment().getId().equals(document.getDepartment().getId()))
                    return;
                break;
            case 4: // Private
                if (document.getCreatedBy().equals(user.getEmail()))
                    return;
                if (privateDocumentRepository.findByUserAndDocumentAndStatus(user, document, 1).isPresent())
                    return;
                break;
        }
        throw new ApiException(ErrorCode.USER_NO_PERMISSION, "User does not have permission to access this document.");
    }

    private void authorizeUserCanEditDocument(User user, Document document) {
        // Hoặc là người tạo, hoặc là quản lý của phòng ban/tổ chức chứa tài liệu
        if (document.getCreatedBy().equals(user.getEmail()))
            return;

        if ((user.getIsOrganizationManager() != null && user.getIsOrganizationManager())
                && user.getOrganization().getId().equals(document.getOrganization().getId()))
            return;

        if ((user.getIsDeptManager() != null && user.getIsDeptManager())
                && user.getDepartment().getId().equals(document.getDepartment().getId()))
            return;

        throw new ApiException(ErrorCode.USER_NO_PERMISSION,
                "User does not have permission to edit or delete this document.");
    }

    private void authorizeUserIsMemberOfOrg(User user, Long orgId) {
        if (user.getOrganization() == null || !user.getOrganization().getId().equals(orgId)) {
            throw new ApiException(ErrorCode.USER_NOT_IN_ORGANIZATION,
                    ErrorMessage.USER_NOT_IN_ORGANIZATION.getMessage());
        }
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND, ErrorMessage.INVALID_USER.getMessage()));
    }

    private Document findDocById(Long docId) {
        return documentRepository.findById(docId)
                .orElseThrow(() -> new ApiException(ErrorCode.DOCUMENT_NOT_FOUND,
                        ErrorMessage.DOCUMENT_NOT_FOUND.getMessage()));
    }

}
