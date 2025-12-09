package com.genifast.dms.service.impl;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.genifast.dms.aop.AuditLog;
import com.genifast.dms.common.constant.ErrorCode;
import com.genifast.dms.common.constant.ErrorMessage;
import com.genifast.dms.common.exception.ApiException;
import com.genifast.dms.common.service.EmailService;
import com.genifast.dms.common.constant.FileSizeFormatter;
import com.genifast.dms.common.utils.JwtUtils;
import com.genifast.dms.dto.request.DocumentCommentRequest;
import com.genifast.dms.dto.request.DocumentCreateRequest;
import com.genifast.dms.dto.request.DocumentFilterRequest;
import com.genifast.dms.dto.request.DocumentShareRequest;
import com.genifast.dms.dto.request.DocumentUpdateRequest;
import com.genifast.dms.dto.request.SearchAndOrNotRequest;
import com.genifast.dms.dto.response.DocumentResponse;
import com.genifast.dms.dto.response.DocumentVersionResponse;
import com.genifast.dms.entity.Category;
import com.genifast.dms.entity.Department;
import com.genifast.dms.entity.Document;
import com.genifast.dms.entity.DocumentVersion;
import com.genifast.dms.entity.User;
import com.genifast.dms.entity.enums.DocumentType;
import com.genifast.dms.entity.DocumentPermission;
import com.genifast.dms.entity.enums.DocumentStatus;
import com.genifast.dms.entity.enums.DocumentConfidentiality;
import com.genifast.dms.mapper.DocumentMapper;
import com.genifast.dms.mapper.DocumentVersionMapper;
import com.genifast.dms.repository.CategoryRepository;
import com.genifast.dms.repository.DepartmentRepository;
import com.genifast.dms.repository.DocumentRepository;
import com.genifast.dms.repository.DocumentPermissionRepository;
import com.genifast.dms.repository.DocumentVersionRepository;
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
    private final DocumentVersionRepository documentVersionRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final CategoryRepository categoryRepository;
    private final PrivateDocumentRepository privateDocumentRepository;
    private final DocumentPermissionRepository documentPermissionRepository;
    private final FileStorageService fileStorageService;
    private final DocumentMapper documentMapper;
    private final DocumentVersionMapper documentVersionMapper;
    private final ObjectMapper objectMapper;
    private final EmailService emailService;

    @Value("${shared.document.base-url}")
    private String baseUrlForSharedDocuments;

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('documents:create') and hasAuthority('documents:upload')")
    @AuditLog(action = "CREATE_UPLOAD_DOCUMENT")
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
                // Các trường content, description giờ sẽ được quản lý bởi version
                .type(StringUtils.getFilenameExtension(file.getOriginalFilename()))
                .documentType(DocumentType.fromCode(createDto.getDocumentType()))
                .category(category)
                .department(category.getDepartment())
                .organization(category.getOrganization())
                .status(DocumentStatus.DRAFT.getValue()) // Trạng thái ban đầu: DRAFT
                .accessType(createDto.getAccessType())
                .filePath(filePath) // Vẫn lưu file path của phiên bản mới nhất để truy cập nhanh
                .fileId(category.getDepartment().getName() + "/" + category.getName())
                .storageCapacity(file.getSize())
                .storageUnit(FileSizeFormatter.format(file.getSize()))
                .originalFilename(file.getOriginalFilename())
                .contentType(file.getContentType())
                .latestVersion(1) // Phiên bản đầu tiên là 1
                .build();

        // Tạo phiên bản đầu tiên (version 1)
        DocumentVersion initialVersion = DocumentVersion.builder()
                .document(doc) // Liên kết ngược lại với document
                .versionNumber(1)
                .title(doc.getTitle())
                .content(createDto.getDescription()) // Lưu description vào version
                .status(doc.getStatus()) // Lưu status vào version
                .changeDescription("Phiên bản ban đầu.")
                .filePath(filePath) // File của phiên bản này
                .build();

        // Thêm phiên bản đầu tiên vào danh sách của Document
        doc.getVersions().add(initialVersion);

        Document savedDoc = documentRepository.save(doc);
        log.info("Document '{}' (ID: {}) and its initial version created by {}", savedDoc.getTitle(), savedDoc.getId(),
                currentUser.getEmail());
        return documentMapper.toDocumentResponse(savedDoc);
    }

    @Override
    @PreAuthorize("hasAuthority('documents:read')")
    @AuditLog(action = "READ_DOCUMENT")
    public DocumentResponse getDocumentMetadata(Long id) {
        User currentUser = findUserByEmail(JwtUtils.getCurrentUserLogin().orElse(""));
        Document document = findDocById(id);
        authorizeUserCanAccessDocument(currentUser, document);
        return documentMapper.toDocumentResponse(document);
    }

    @Override
    @PreAuthorize("hasAuthority('documents:download')")
    @AuditLog(action = "DOWNLOAD_DOCUMENT")
    public ResponseEntity<Resource> downloadDocumentFile(Long id) {
        User currentUser = findUserByEmail(JwtUtils.getCurrentUserLogin().orElse(""));
        Document document = findDocById(id);
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
    @PreAuthorize("hasPermission(#id, 'document', 'documents:update')")
    @AuditLog(action = "UPDATE_DOCUMENT")
    public DocumentResponse updateDocumentMetadata(Long id, DocumentUpdateRequest updateDto) {
        User currentUser = findUserByEmail(JwtUtils.getCurrentUserLogin().orElse(""));
        Document document = findDocById(id);
        authorizeUserCanEditDocument(currentUser, document);

        // **LOGIC VERSIONING MỚI**
        // 1. Tăng số phiên bản
        int newVersionNumber = document.getLatestVersion() + 1;
        document.setLatestVersion(newVersionNumber);

        // 2. Cập nhật thông tin trên Document chính (luôn là thông tin mới nhất)
        document.setTitle(updateDto.getTitle());
        if (updateDto.getCategoryId() != null && !updateDto.getCategoryId().equals(document.getCategory().getId())) {
            Category newCategory = categoryRepository.findById(updateDto.getCategoryId())
                    .orElseThrow(() -> new ApiException(ErrorCode.CATEGORY_NOT_FOUND,
                            ErrorMessage.CATEGORY_NOT_FOUND.getMessage()));
            document.setCategory(newCategory);
        }
        // Giả sử sau khi update, tài liệu quay về trạng thái PENDING để được duyệt lại
        document.setStatus(2); // PENDING

        // 3. Tạo một bản ghi DocumentVersion mới
        DocumentVersion newVersion = DocumentVersion.builder()
                .document(document)
                .versionNumber(newVersionNumber)
                .title(updateDto.getTitle())
                .content(updateDto.getDescription())
                .status(document.getStatus()) // Trạng thái của phiên bản này là PENDING
                .changeDescription(updateDto.getChangeDescription()) // Cần thêm trường này vào DTO
                .filePath(document.getFilePath()) // Giả sử file không thay đổi, nếu có thì cần logic upload file mới
                .build();

        // 4. Thêm phiên bản mới vào tài liệu
        document.getVersions().add(newVersion);

        Document updatedDoc = documentRepository.save(document);
        log.info("Document ID {} metadata updated by {}", id, currentUser.getEmail());
        return documentMapper.toDocumentResponse(updatedDoc);
    }

    @Override
    @Transactional
    @PreAuthorize("hasPermission(#id, 'document', 'documents:delete')")
    @AuditLog(action = "DELETE_DOCUMENT")
    public void deleteDocument(Long id) {
        log.info("Nghiệp vụ xóa tài liệu ID: {}", id);
        // 1) Tìm document theo ID (throw nếu không tồn tại)
        Document document = findDocById(id);

        // 2) Xóa các bản ghi phân quyền riêng tư liên quan (nếu có)
        try {
            privateDocumentRepository.deleteByDocument(document);
        } catch (Exception ex) {
            log.warn("Không thể xóa PrivateDoc liên quan đến document ID {}: {}", id, ex.getMessage());
        }

        // 3) Xóa file vật lý nếu có fileId
        String fileId = document.getFileId();
        if (fileId != null && !fileId.isEmpty()) {
            try {
                fileStorageService.deleteFileById(fileId);
            } catch (Exception ex) {
                // Không chặn việc xóa Document nếu xóa file vật lý thất bại
                log.warn("Xóa file vật lý thất bại cho document ID {} (fileId={}): {}", id, fileId, ex.getMessage());
            }
        }

        // 4) Xóa bản ghi Document
        documentRepository.delete(document);
        log.info("Đã xóa document ID {} khỏi cơ sở dữ liệu", id);
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

    @Override
    @Transactional
    @PreAuthorize("hasPermission(#id, 'document', 'documents:approve')")
    @AuditLog(action = "APPROVE_DOCUMENT")
    public DocumentResponse approveDocument(Long docId) {
        Document document = findDocById(docId);

        // ABAC Check: Chỉ phê duyệt khi tài liệu đang ở trạng thái PENDING (giả sử là
        // 2)
        if (document.getStatus() != 2) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "Tài liệu không ở trạng thái chờ phê duyệt.");
        }

        document.setStatus(DocumentStatus.APPROVED.getValue()); // 3 = APPROVED
        Document approvedDoc = documentRepository.save(document);
        log.info("Document ID {} has been approved by {}", docId, JwtUtils.getCurrentUserLogin().orElse(""));

        // --- BẮT ĐẦU PHẦN GỬI EMAIL ---
        User creator = findUserByEmail(approvedDoc.getCreatedBy());
        String subject = "Tài liệu của bạn đã được phê duyệt";
        String message = String.format(
                "Tài liệu <strong>'%s'</strong> (ID: %d) mà bạn đã trình duyệt đã được phê duyệt thành công.",
                approvedDoc.getTitle(), approvedDoc.getId());
        emailService.sendDocumentNotification(creator.getEmail(), creator.getFirstName(), subject, message);
        // --- KẾT THÚC PHẦN GỬI EMAIL ---

        return documentMapper.toDocumentResponse(approvedDoc);
    }

    @Override
    @Transactional
    @PreAuthorize("hasPermission(#id, 'document', 'documents:reject')")
    @AuditLog(action = "REJECT_DOCUMENT")
    public DocumentResponse rejectDocument(Long docId, String reason) {
        Document document = findDocById(docId);

        // ABAC Check: Chỉ từ chối khi tài liệu đang ở trạng thái PENDING (giả sử là 2)
        if (document.getStatus() != 2) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "Tài liệu không ở trạng thái chờ phê duyệt.");
        }

        if (!StringUtils.hasText(reason)) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "Lý do từ chối là bắt buộc.");
        }

        document.setStatus(DocumentStatus.REJECTED.getValue()); // 4 = REJECTED
        // Bổ sung: Lưu lại lý do từ chối. Cần thêm một trường vào Document entity, ví
        // dụ: rejectReason
        document.setRejectReason(reason);

        Document rejectedDoc = documentRepository.save(document);
        log.warn("Document ID {} was rejected by {} with reason: {}", docId, JwtUtils.getCurrentUserLogin().orElse(""),
                reason);

        // --- BẮT ĐẦU PHẦN GỬI EMAIL ---
        User creator = findUserByEmail(rejectedDoc.getCreatedBy());
        String subject = "Tài liệu của bạn đã bị từ chối";
        String message = String.format(
                "Tài liệu <strong>'%s'</strong> (ID: %d) của bạn đã bị từ chối.<br/><strong>Lý do:</strong> %s",
                rejectedDoc.getTitle(), rejectedDoc.getId(), reason);
        emailService.sendDocumentNotification(creator.getEmail(), creator.getFirstName(), subject, message);
        // --- KẾT THÚC PHẦN GỬI EMAIL ---

        return documentMapper.toDocumentResponse(rejectedDoc);
    }

    @Override
    @Transactional
    @PreAuthorize("isAuthenticated()")
    @AuditLog(action = "SHARE_DOCUMENT")
    public void shareDocument(Long id, DocumentShareRequest shareRequest) {
        log.info(" nghiệp vụ chia sẻ tài liệu ID: {} với người dùng ID {}", id, shareRequest.getRecipientId());

        // Check specific permissions first
        User currentUser = findUserByEmail(JwtUtils.getCurrentUserLogin().orElse(""));

        Document document = findDocById(id);

        // Only owner (uploader) can share their documents
        if (document.getCreatedBy() == null || !document.getCreatedBy().equals(currentUser.getEmail())) {
            throw new ApiException(ErrorCode.ACCESS_DENIED, "Only the owner can share this document.");
        }

        // Validate recipient is required (by ID only)
        Long recipientId = shareRequest.getRecipientId();
        if (recipientId == null) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "recipient_id is required");
        }

        // Validate permissions: must be present and only allowed base permissions
        List<String> basePerms = List.of(
                "documents:share:readonly",
                "documents:share:forwardable",
                "documents:share:shareable");
        List<String> incomingPerms = shareRequest.getPermissions();
        if (incomingPerms == null || incomingPerms.isEmpty()) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "permissions is required and must not be empty");
        }
        for (String p : incomingPerms) {
            if (!basePerms.contains(p)) {
                throw new ApiException(ErrorCode.INVALID_REQUEST, "permissions contains unsupported value: " + p);
            }
        }

        // Check if current user has all requested permissions
        for (String requestedPerm : incomingPerms) {
            if (!validateSharePermission(currentUser, document, requestedPerm)) {
                log.warn("User {} attempted to share with permission {} but doesn't have it",
                        currentUser.getEmail(), requestedPerm);
                throw new ApiException(ErrorCode.ACCESS_DENIED, ErrorMessage.NO_PERMISSION.getMessage());
            }
        }

        // Load recipient & basic checks
        User recipient = userRepository.findById(recipientId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND,
                        ErrorMessage.RECIPIENT_NOT_FOUND.getMessage()));
        if (recipient.getStatus() != null && recipient.getStatus() != 1) {
            throw new ApiException(ErrorCode.ACCESS_DENIED, "Recipient not active.");
        }

        // Validate document status must be APPROVED
        if (document.getStatus() == null || !document.getStatus().equals(DocumentStatus.APPROVED.getValue())) {
            throw new ApiException(ErrorCode.ACCESS_DENIED,
                    ErrorMessage.DOCUMENT_NOT_APPROVED_FOR_SHARING.getMessage());
        }

        // Determine actual scope by organization relationship
        boolean sameOrg = currentUser.getOrganization() != null && recipient.getOrganization() != null
                && currentUser.getOrganization().getId().equals(recipient.getOrganization().getId());
        boolean requestedExternal = Boolean.TRUE.equals(shareRequest.getIsShareToExternal());
        boolean actualExternal = !sameOrg;

        // Check document access_type first to determine sharing rules
        Integer accessType = document.getAccessType();
        boolean isExternalDocument = accessType != null && accessType == 1; // EXTERNAL access_type

        // Auto-detect external sharing for EXTERNAL documents
        if (isExternalDocument && actualExternal && !requestedExternal) {
            requestedExternal = true; // Auto-set for EXTERNAL documents shared externally
            log.info("Auto-detected external sharing for EXTERNAL document ID {} to external user {}",
                    id, recipient.getEmail());
        }

        // For EXTERNAL documents (access_type=1), external sharing is allowed based on
        // document type (ABAC)
        // No need to check user permissions since documents:share:external is purely
        // ABAC
        if (!isExternalDocument) {
            // For non-EXTERNAL documents, enforce organization relationship matching
            if (requestedExternal != actualExternal) {
                throw new ApiException(ErrorCode.INVALID_REQUEST,
                        ErrorMessage.CANNOT_SHARE_INTERNAL_EXTERNALLY.getMessage());
            }
        }

        // Access type suitability rules
        if (requestedExternal) {
            // External: only PUBLIC(2) or EXTERNAL(1)
            if (accessType != null && !(accessType == 2 || accessType == 1)) {
                throw new ApiException(ErrorCode.ACCESS_DENIED,
                        "Document access type not suitable for external sharing.");
            }
        } else {
            // Internal: allow PUBLIC(2), INTERNAL(3); block EXTERNAL(1), and guard
            // PRIVATE(4) via private_docs
            if (accessType != null && accessType == 1) {
                throw new ApiException(ErrorCode.ACCESS_DENIED,
                        "Document access type not suitable for internal sharing.");
            }
        }

        // PRIVATE access_type(4) requires explicit authorization via private_docs; also
        // disallow external for these
        if (accessType != null && accessType == 4) {
            if (requestedExternal) {
                throw new ApiException(ErrorCode.ACCESS_DENIED, "Private document cannot be shared externally.");
            }
            boolean recipientAuthorized = privateDocumentRepository
                    .findByUserAndDocumentAndStatus(recipient, document, 1)
                    .isPresent();
            if (!recipientAuthorized) {
                throw new ApiException(ErrorCode.ACCESS_DENIED,
                        ErrorMessage.RECIPIENT_NOT_AUTHORIZED_PRIVATE.getMessage());
            }
        }

        // Validate timebound inputs
        Instant toDate = shareRequest.getToDate();
        Instant fromDate = shareRequest.getFromDate();
        if ((fromDate == null) ^ (toDate == null)) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "from_date and to_date must be provided together");
        }
        if (fromDate != null && toDate != null && toDate.isBefore(fromDate)) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, ErrorMessage.INVALID_DATE_RANGE.getMessage());
        }

        // If timebound sharing is requested, user must have timebound permission
        if (fromDate != null && toDate != null) {
            if (!validateSharePermission(currentUser, document, "documents:share:timebound")) {
                log.warn("User {} attempted timebound sharing but doesn't have documents:share:timebound permission",
                        currentUser.getEmail());
                throw new ApiException(ErrorCode.ACCESS_DENIED, ErrorMessage.NO_PERMISSION.getMessage());
            }
        }

        // PRIVATE documents require timebound sharing with specific dates
        if (document.getAccessType() != null && document.getAccessType() == 4) {
            if (fromDate == null || toDate == null) {
                throw new ApiException(ErrorCode.INVALID_REQUEST,
                        ErrorMessage.PRIVATE_DOCUMENT_REQUIRES_TIMEBOUND.getMessage());
            }
        }

        // Build permissions to persist
        List<String> finalPerms = new ArrayList<>(incomingPerms);

        // Auto-add timebound only when both from/to provided
        if (fromDate != null && toDate != null) {
            if (!finalPerms.contains("documents:share:timebound")) {
                finalPerms.add("documents:share:timebound");
            }
        }

        // Add scope permission based on final requestedExternal value
        if (requestedExternal) {
            if (!finalPerms.contains("documents:share:external")) {
                finalPerms.add("documents:share:external");
            }
        } else {
            if (!finalPerms.contains("documents:share:orgscope")) {
                finalPerms.add("documents:share:orgscope");
            }
        }

        // Replace existing permissions for this recipient & document to avoid
        // duplicates
        documentPermissionRepository.deleteByUserIdAndDocId(recipientId, id);

        // Persist one row per permission
        for (String perm : finalPerms) {
            DocumentPermission dp = DocumentPermission.builder()
                    .userId(recipientId)
                    .docId(id)
                    .permission(perm)
                    .version(document.getVersion())
                    .expiryDate((fromDate != null && toDate != null) ? toDate : null)
                    .build();
            documentPermissionRepository.save(dp);
        }
    }

    @Override
    @PreAuthorize("hasPermission(#docId, 'document', 'documents:track')")
    @AuditLog(action = "TRACK_DOCUMENT")
    public void trackDocumentHistory(Long docId) {
        // Phương thức này không cần làm gì cả.
        // Annotation @PreAuthorize đã kiểm tra quyền.
        // Annotation @AuditLog sẽ tự động ghi lại hành động "TRACK_DOCUMENT".
        // Logic hiển thị log sẽ nằm ở AuditLogController.
        log.info("User {} is tracking history for document ID: {}. Action logged.",
                JwtUtils.getCurrentUserLogin().orElse(""), docId);
    }

    @Override
    @Transactional
    @PreAuthorize("hasPermission(#id, 'document', 'documents:submit')")
    @AuditLog(action = "SUBMIT_DOCUMENT")
    public DocumentResponse submitDocument(Long docId) {
        Document document = findDocById(docId);

        // ABAC Check: Chỉ trình duyệt khi tài liệu đang ở trạng thái DRAFT (giả sử là
        // 1)
        if (document.getStatus() != 1) {
            throw new ApiException(ErrorCode.INVALID_REQUEST,
                    "Chỉ có thể trình duyệt tài liệu ở trạng thái bản nháp (Draft).");
        }

        document.setStatus(DocumentStatus.PENDING.getValue()); // 2 = PENDING
        Document submittedDoc = documentRepository.save(document);
        log.info("Document ID {} has been submitted for approval by {}", docId,
                JwtUtils.getCurrentUserLogin().orElse(""));

        // --- BẮT ĐẦU PHẦN GỬI EMAIL ---
        // Tìm tất cả các manager của phòng ban để gửi thông báo
        Department docDepartment = submittedDoc.getDepartment();
        if (docDepartment != null) {
            docDepartment.getUsers().stream()
                    .filter(user -> user.getIsDeptManager() != null && user.getIsDeptManager())
                    .forEach(manager -> {
                        String subject = "Có tài liệu mới cần bạn phê duyệt";
                        String message = String.format(
                                "Tài liệu <strong>'%s'</strong> (ID: %d) vừa được trình duyệt bởi <strong>%s</strong> và đang chờ bạn xử lý.",
                                submittedDoc.getTitle(), submittedDoc.getId(), submittedDoc.getCreatedBy());
                        emailService.sendDocumentNotification(manager.getEmail(), manager.getFirstName(), subject,
                                message);
                    });
        }
        // --- KẾT THÚC PHẦN GỬI EMAIL ---

        return documentMapper.toDocumentResponse(submittedDoc);
    }

    @Override
    @Transactional
    @PreAuthorize("hasPermission(#id, 'document', 'documents:publish')")
    @AuditLog(action = "PUBLISH_DOCUMENT")
    public DocumentResponse publishDocument(Long docId) {
        Document document = findDocById(docId);

        // Logic ABAC đã được kiểm tra trong PermissionEvaluator,
        // nhưng để an toàn, chúng ta có thể kiểm tra lại ở đây.
        if (document.getDocumentType() != DocumentType.NOTICE || document.getStatus() != 3) {
            throw new ApiException(ErrorCode.INVALID_REQUEST,
                    "Chỉ có thể công khai các tài liệu loại 'Thông báo' đã được phê duyệt.");
        }

        document.setAccessType(1); // 1 = PUBLIC
        Document publishedDoc = documentRepository.save(document);
        log.info("Document ID {} has been published by {}", docId, JwtUtils.getCurrentUserLogin().orElse(""));

        // TODO: Có thể cần gửi thông báo đến toàn bộ tổ chức.

        return documentMapper.toDocumentResponse(publishedDoc);
    }

    @Override
    @Transactional
    @PreAuthorize("hasPermission(#docId, 'document', 'documents:archive')")
    @AuditLog(action = "ARCHIVE_DOCUMENT")
    public DocumentResponse archiveDocument(Long id) {
        Document document = findDocById(id);

        // Check if document is already archived
        if (document.getStatus() != null && document.getStatus().equals(DocumentStatus.ARCHIVED.getValue())) {
            log.info("Document ID {} is already in ARCHIVED status", id);
            return documentMapper.toDocumentResponse(document);
        }

        document.setStatus(DocumentStatus.ARCHIVED.getValue());
        document.setArchivedAt(Instant.now());
        Document archivedDoc = documentRepository.save(document);

        log.info("Document ID {} has been archived by {}", id, JwtUtils.getCurrentUserLogin().orElse(""));
        return documentMapper.toDocumentResponse(archivedDoc);
    }

    @Override
    @Transactional
    @PreAuthorize("hasPermission(#id, 'document', 'documents:sign')")
    @AuditLog(action = "SIGN_DOCUMENT")
    public DocumentResponse signDocument(Long id) {
        Document document = findDocById(id);

        // Set signing metadata
        document.setSignedAt(Instant.now());
        document.setSignedBy(findUserByEmail(JwtUtils.getCurrentUserLogin().orElse("")));

        Document signedDoc = documentRepository.save(document);

        log.info("Document ID {} has been signed by {}", id, JwtUtils.getCurrentUserLogin().orElse(""));
        return documentMapper.toDocumentResponse(signedDoc);
    }

    @Override
    @Transactional
    @PreAuthorize("hasPermission(#id, 'document', 'documents:lock')")
    @AuditLog(action = "LOCK_DOCUMENT")
    public DocumentResponse lockDocument(Long id) {
        Document document = findDocById(id);
        document.setStatus(DocumentStatus.LOCKED.getValue());
        Document lockedDoc = documentRepository.save(document);
        log.info("Document ID {} has been locked by {}", id, JwtUtils.getCurrentUserLogin().orElse(""));
        return documentMapper.toDocumentResponse(lockedDoc);
    }

    @Override
    @Transactional
    @PreAuthorize("hasPermission(#id, 'document', 'documents:unlock')")
    @AuditLog(action = "UNLOCK_DOCUMENT")

    public DocumentResponse unlockDocument(Long id) {
        Document document = findDocById(id);

        // Return to APPROVED status after unlocking
        if (document.getStatus() != null && document.getStatus().equals(DocumentStatus.LOCKED.getValue())) {
            document.setStatus(DocumentStatus.APPROVED.getValue());
        }

        Document unlockedDoc = documentRepository.save(document);
        log.info("Document ID {} has been unlocked by {}", id, JwtUtils.getCurrentUserLogin().orElse(""));
        return documentMapper.toDocumentResponse(unlockedDoc);
    }

    @Override
    @Transactional
    @PreAuthorize("hasPermission(#id, 'document', 'documents:comment')")
    @AuditLog(action = "ADD_COMMENT")
    public void addComment(Long docId, DocumentCommentRequest commentRequest) {
        // TODO: Implement logic tạo và lưu entity Comment liên kết với Document và User
        log.info("Thêm bình luận vào tài liệu ID: {}", docId);
    }

    @Override
    @Transactional
    @PreAuthorize("hasPermission(#docId, 'document', 'documents:restore')")
    @AuditLog(action = "RESTORE_DOCUMENT")
    public DocumentResponse restoreDocument(Long id) {
        Document document = findDocById(id);

        // Only restore if document is archived
        if (document.getStatus() != null && document.getStatus().equals(DocumentStatus.ARCHIVED.getValue())) {
            document.setStatus(DocumentStatus.APPROVED.getValue()); // Restore to APPROVED
            document.setArchivedAt(null); // Clear archived timestamp
        }

        Document restoredDoc = documentRepository.save(document);
        log.info("Document ID {} has been restored by {}", id, JwtUtils.getCurrentUserLogin().orElse(""));
        return documentMapper.toDocumentResponse(restoredDoc);
    }

    @Override
    @PreAuthorize("hasPermission(#id, 'document', 'documents:history')")
    @AuditLog(action = "VIEW_HISTORY")
    public List<DocumentVersionResponse> getDocumentVersions(Long id) {
        log.info("Nghiệp vụ xem lịch sử các phiên bản của tài liệu ID: {}", id);
        Document document = findDocById(id);
        if (documentVersionRepository == null) {
            log.warn("DocumentVersionRepository chưa được tiêm. Trả danh sách rỗng cho getDocumentVersions.");
            return Collections.emptyList();
        }
        List<DocumentVersion> versions = documentVersionRepository
                .findByDocumentIdOrderByVersionNumberDesc(document.getId());
        // return versions.stream().map(v -> {
        // DocumentVersionResponse resp = new DocumentVersionResponse();
        // resp.setDocumentId(document.getId());
        // resp.setVersionNumber(v.getVersionNumber());
        // resp.setTitle(v.getTitle());
        // resp.setDescription(v.getDescription());
        // resp.setCreatedBy(v.getCreatedBy());
        // resp.setCreatedAt(v.getCreatedAt());
        // return resp;
        // }).toList();

        return documentVersionMapper.toResponseList(versions);
    }

    @Override
    @PreAuthorize("hasPermission(#id, 'document', 'documents:version:read')")
    @AuditLog(action = "VIEW_SPECIFIC_VERSION")
    public DocumentVersionResponse getSpecificDocumentVersion(Long id, Integer versionNumber) {
        log.info("Nghiệp vụ xem phiên bản cụ thể số {} của tài liệu ID: {}", versionNumber, id);
        Document document = findDocById(id);
        // Kiểm tra quyền truy cập tổng thể vào tài liệu
        User currentUser = findUserByEmail(JwtUtils.getCurrentUserLogin().orElse(""));
        authorizeUserCanAccessDocument(currentUser, document);

        if (documentVersionRepository == null) {
            throw new ApiException(ErrorCode.DOCUMENT_NOT_FOUND, "Document version repository not available.");
        }
        DocumentVersion version = documentVersionRepository
                .findByDocumentIdAndVersionNumber(document.getId(), versionNumber)
                .orElseThrow(() -> new ApiException(ErrorCode.DOCUMENT_NOT_FOUND, "Document version not found."));

        // DocumentVersionResponse resp = new DocumentVersionResponse();
        // resp.setDocumentId(document.getId());
        // resp.setVersionNumber(version.getVersionNumber());
        // resp.setTitle(version.getTitle());
        // resp.setDescription(version.getDescription());
        // resp.setCreatedBy(version.getCreatedBy());
        // resp.setCreatedAt(version.getCreatedAt());
        return documentVersionMapper.toResponse(version);
    }

    @Override
    @PreAuthorize("hasPermission(#id, 'document', 'documents:notify')")
    @AuditLog(action = "NOTIFY_RECIPIENTS")
    public void notifyRecipients(Long docId, String message) {
        Document document = findDocById(docId);

        if (document.getRecipients() == null || document.getRecipients().isEmpty()) {
            log.info("Document ID {} has no recipients to notify.", docId);
            return;
        }

        log.info("Sending notification for document ID: {} with message: '{}'", docId, message);

        String subject = "Thông báo quan trọng về tài liệu: " + document.getTitle();

        document.getRecipients().forEach(recipient -> {
            emailService.sendDocumentNotification(recipient.getEmail(), recipient.getFirstName(), subject,
                    message);
        });
    }

    @Override
    @PreAuthorize("hasPermission(#id, 'document', 'documents:export')")
    @AuditLog(action = "EXPORT_DOCUMENT")
    public ResponseEntity<Resource> exportDocument(Long id, String format) {
        log.info("Exporting document ID: {} to format: {}", id, format);
        // Logic này có thể rất phức tạp, ở đây ta giả lập việc export ra file text
        Document document = findDocById(id);
        String content = "DOCUMENT EXPORT\n" +
                "ID: " + document.getId() + "\n" +
                "Title: " + document.getTitle() + "\n" +
                "Description: " + document.getDescription();

        Resource resource = new ByteArrayResource(content.getBytes());
        String filename = "export-" + document.getId() + "." + format;

        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_PLAIN)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(resource);
    }

    @Override
    @Transactional
    @PreAuthorize("hasPermission(#id, 'document', 'documents:forward')")
    @AuditLog(action = "FORWARD_DOCUMENT")
    public void forwardDocument(Long docId, String recipientEmail) {
        Document document = findDocById(docId);
        User userToForward = findUserByEmail(recipientEmail);

        // ABAC Check: Người nhận chuyển tiếp phải cùng tổ chức
        if (userToForward.getOrganization() == null ||
                !userToForward.getOrganization().getId().equals(document.getOrganization().getId())) {
            throw new ApiException(ErrorCode.USER_NOT_IN_ORGANIZATION,
                    "Chỉ có thể chuyển tiếp cho người dùng trong cùng tổ chức.");
        }

        // Thêm người dùng vào danh sách người nhận và lưu lại
        document.getRecipients().add(userToForward);
        documentRepository.save(document);

        log.info("Document ID {} forwarded to user '{}' by {}", docId, recipientEmail,
                JwtUtils.getCurrentUserLogin().orElse(""));

        // --- BẮT ĐẦU PHẦN GỬI EMAIL ---
        User forwarder = findUserByEmail(JwtUtils.getCurrentUserLogin().orElse(""));
        User recipient = findUserByEmail(recipientEmail); // Cần lấy object User để có tên
        String subject = "Bạn vừa được chuyển tiếp một tài liệu";
        String message = String.format(
                "Bạn vừa được <strong>%s</strong> chuyển tiếp tài liệu <strong>'%s'</strong> (ID: %d).",
                forwarder.getFirstName(), document.getTitle(), document.getId());
        emailService.sendDocumentNotification(recipientEmail, recipient.getFirstName(), subject, message);
        // --- KẾT THÚC PHẦN GỬI EMAIL ---
    }

    @Override
    @Transactional
    @PreAuthorize("hasPermission(#id, 'document', 'documents:distribute')")
    @AuditLog(action = "DISTRIBUTE_DOCUMENT")
    public void distributeDocument(Long docId, List<Long> departmentIds) {
        if (departmentIds == null || departmentIds.isEmpty()) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "Cần chỉ định ít nhất một phòng ban để phân phối.");
        }

        Document document = findDocById(docId);
        User currentUser = findUserByEmail(JwtUtils.getCurrentUserLogin().orElse(""));

        // Lấy danh sách người dùng từ các phòng ban được chỉ định
        List<User> usersToReceive = userRepository.findAllById(
                departmentRepository.findAllById(departmentIds).stream()
                        .flatMap(department -> department.getUsers().stream())
                        .map(User::getId)
                        .collect(Collectors.toList()));

        // Thêm những người dùng này vào danh sách người nhận của tài liệu
        document.getRecipients().addAll(usersToReceive);
        documentRepository.save(document);

        log.info("Document ID {} distributed to {} users in {} departments by {}",
                docId, usersToReceive.size(), departmentIds.size(), currentUser.getEmail());

        // --- BẮT ĐẦU PHẦN GỬI EMAIL ---
        String subject = "Thông báo: Tài liệu mới được phân phối";
        String message = String.format(
                "Tài liệu quan trọng <strong>'%s'</strong> (ID: %d) vừa được phân phối đến phòng ban của bạn.",
                document.getTitle(), document.getId());
        usersToReceive.forEach(
                user -> emailService.sendDocumentNotification(user.getEmail(), user.getFirstName(), subject, message));
        // --- KẾT THÚC PHẦN GỬI EMAIL ---
    }

    @Override
    @PreAuthorize("hasAuthority('documents:report')") // Chỉ user có quyền mới được vào
    @AuditLog(action = "GENERATE_DOCUMENT_REPORT")
    public ResponseEntity<Resource> generateDocumentReport(String reportType, Long departmentId) {
        User currentUser = findUserByEmail(JwtUtils.getCurrentUserLogin().orElse(""));
        Department department = departmentRepository.findById(departmentId).orElseThrow(
                () -> new ApiException(ErrorCode.DEPARTMENT_NOT_FOUND, ErrorMessage.DEPARTMENT_NOT_FOUND.getMessage()));

        // ABAC check: CustomPermissionEvaluator sẽ không chạy với endpoint không có
        // {id},
        // ta phải tự kiểm tra ở đây.
        boolean isOrgManager = currentUser.getIsOrganizationManager() != null && currentUser.getIsOrganizationManager()
                && currentUser.getOrganization().getId().equals(department.getOrganization().getId());
        boolean isDeptManager = currentUser.getIsDeptManager() != null && currentUser.getIsDeptManager()
                && currentUser.getDepartment().getId().equals(department.getId());
        if (!isOrgManager && !isDeptManager) {
            throw new ApiException(ErrorCode.USER_NO_PERMISSION, "Không có quyền tạo báo cáo cho phòng ban này.");
        }

        log.info("Generating report type '{}' for department ID: {}", reportType, departmentId);
        // Giả lập logic tạo báo cáo
        List<Document> documents = documentRepository.findByDepartmentId(departmentId);
        StringBuilder csvContent = new StringBuilder("ID,Title,Status,CreatedBy,CreatedAt\n");
        for (Document doc : documents) {
            csvContent.append(String.format("%d,%s,%d,%s,%s\n",
                    doc.getId(), doc.getTitle(), doc.getStatus(), doc.getCreatedBy(), doc.getCreatedAt().toString()));
        }

        Resource resource = new ByteArrayResource(csvContent.toString().getBytes());
        String filename = "report-" + reportType + "-" + departmentId + ".csv";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(resource);
    }

    @Override
    @Transactional
    @PreAuthorize("hasPermission(#id, 'document', 'documents:share:external')")
    @AuditLog(action = "SHARE_DOCUMENT")
    public String createShareLink(Long id, Instant expiryAt, boolean allowDownload) {
        User currentUser = findUserByEmail(JwtUtils.getCurrentUserLogin().orElse(""));
        Document document = findDocById(id);

        // Người dùng phải có quyền truy cập vào tài liệu này mới được chia sẻ
        authorizeUserCanAccessDocument(currentUser, document);

        // Tạo token ngẫu nhiên
        String token = UUID.randomUUID().toString();
        document.setShareToken(token);
        document.setPublicShareExpiryAt(expiryAt);
        document.setAllowPublicDownload(allowDownload);

        documentRepository.save(document);
        log.info("User {} created a share link for document ID {}",
                currentUser.getEmail(), id);

        // Trả về URL đầy đủ để hiển thị cho người dùng
        return baseUrlForSharedDocuments + token;
    }

    // ------ Helper Method ------
    private void authorizeUserCanAccessDocument(User user, Document document) {
        // Lấy thông tin thiết bị từ request header
        HttpServletRequest request = getCurrentRequest();
        String deviceType = request != null ? request.getHeader("Device-Type") : "UNKNOWN";

        // Quy tắc ABAC theo Device Type: Tài liệu PRIVATE access_type chỉ được truy cập
        // từ thiết bị COMPANY_DEVICE
        log.info("Checking access for document ID: {} with access_type: {} and device type: {}", document.getId(),
                document.getAccessType(), deviceType);
        if (document.getAccessType() != null && document.getAccessType() == 4 && !"COMPANY_DEVICE".equals(deviceType)) {
            throw new ApiException(ErrorCode.ACCESS_DENIED, "Access denied from external device for private document.");
        }

        // Cho phép override bằng bản ghi được chia sẻ (private_docs) CHO MỌI accessType
        // Điều này phục vụ tình huống 8.2: tài liệu phòng ban được chia sẻ cho người ở
        // phòng ban khác
        if (privateDocumentRepository.findByUserAndDocumentAndStatus(user, document, 1).isPresent()) {
            return;
        }

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
                // Cho phép quản lý tổ chức đọc tài liệu PRIVATE nếu truy cập từ thiết bị công
                // ty
                if (Boolean.TRUE.equals(user.getIsOrganizationManager()))
                    return;
                if (document.getCreatedBy().equals(user.getEmail()))
                    return;
                break;
        }
        throw new ApiException(ErrorCode.ACCESS_DENIED, "User does not have permission to access this document.");
    }

    // Helper method to get current HttpServletRequest
    private HttpServletRequest getCurrentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes) {
            return ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        }
        return null;
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

    /**
     * Validate share permission based on document access_type and user context
     */
    private boolean validateSharePermission(User user, Document document, String permission) {
        // documents:share:external is purely ABAC - no validation needed here
        // It's handled in the shareDocument method based on document access_type
        if ("documents:share:external".equals(permission)) {
            return true; // Always allow - ABAC logic is in shareDocument method
        }

        // For other permissions, check RBAC first (JWT token)
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            boolean hasAuthority = authentication.getAuthorities().stream()
                    .anyMatch(authority -> authority.getAuthority().equals(permission));
            if (!hasAuthority) {
                log.warn("User {} does not have authority {} in their JWT token", user.getEmail(), permission);
                return false;
            }
        } else {
            log.warn("No authentication context found for user {}", user.getEmail());
            return false;
        }

        // Then check document-level constraints (ABAC)
        switch (permission) {
            case "documents:share:readonly":
                // Check if user can access this document for reading
                try {
                    authorizeUserCanAccessDocument(user, document);
                    return true;
                } catch (Exception e) {
                    return false;
                }

            case "documents:share:forwardable":
                // Check if document allows forwarding based on access_type
                Integer accessType = document.getAccessType();
                // PRIVATE(4) access type documents cannot be forwarded
                if (accessType != null && accessType == 4) {
                    log.warn("Document ID {} with access_type PRIVATE cannot be forwarded", document.getId());
                    return false;
                }
                // User must have access to document to forward it
                try {
                    authorizeUserCanAccessDocument(user, document);
                    return true;
                } catch (Exception e) {
                    return false;
                }

            case "documents:share:shareable":
                // Check if document is shareable based on access type
                Integer docAccessType = document.getAccessType();

                // PRIVATE(4) access type documents have restricted sharing
                if (docAccessType != null && docAccessType == 4) {
                    // Only creator or explicitly authorized users can share private docs
                    boolean isCreator = document.getCreatedBy() != null &&
                            document.getCreatedBy().equals(user.getEmail());
                    if (!isCreator) {
                        boolean hasPrivateAccess = privateDocumentRepository
                                .findByUserAndDocumentAndStatus(user, document, 1)
                                .isPresent();
                        if (!hasPrivateAccess) {
                            log.warn("User {} cannot share PRIVATE document ID {} - not creator and no private access",
                                    user.getEmail(), document.getId());
                            return false;
                        }
                    }
                }

                // User must have access to document to share it
                try {
                    authorizeUserCanAccessDocument(user, document);
                    return true;
                } catch (Exception e) {
                    return false;
                }

            case "documents:share:timebound":
                // Timebound permission is used for time-limited sharing
                // User must have access to document to set timebound sharing
                try {
                    authorizeUserCanAccessDocument(user, document);
                    return true;
                } catch (Exception e) {
                    return false;
                }

            default:
                log.warn("Unknown permission: {}", permission);
                return false;
        }
    }

}
