package com.genifast.dms.service.impl;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.genifast.dms.aop.AuditLog;
import com.genifast.dms.common.constant.ErrorCode;
import com.genifast.dms.common.constant.ErrorMessage;
import com.genifast.dms.common.constant.FileSizeFormatter;
import com.genifast.dms.common.exception.ApiException;
import com.genifast.dms.common.service.EmailService;
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
import com.genifast.dms.mapper.DocumentMapper;
import com.genifast.dms.mapper.DocumentVersionMapper;
import com.genifast.dms.repository.CategoryRepository;
import com.genifast.dms.repository.DepartmentRepository;
import com.genifast.dms.repository.DocumentRepository;
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
                .status(1) // Trạng thái ban đầu: DRAFT
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
    public DocumentResponse getDocumentMetadata(Long docId) {
        User currentUser = findUserByEmail(JwtUtils.getCurrentUserLogin().orElse(""));
        Document document = findDocById(docId);
        authorizeUserCanAccessDocument(currentUser, document);
        return documentMapper.toDocumentResponse(document);
    }

    @Override
    @PreAuthorize("hasAuthority('documents:download')")
    @AuditLog(action = "DOWNLOAD_DOCUMENT")
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
    @PreAuthorize("hasPermission(#docId, 'document', 'documents:update')")
    @AuditLog(action = "UPDATE_DOCUMENT")
    public DocumentResponse updateDocumentMetadata(Long docId, DocumentUpdateRequest updateDto) {
        User currentUser = findUserByEmail(JwtUtils.getCurrentUserLogin().orElse(""));
        Document document = findDocById(docId);

        // Logic kiểm tra quyền chỉnh sửa vẫn giữ nguyên
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
        log.info("Document ID {} updated to version {} by {}", docId, newVersionNumber, currentUser.getEmail());
        return documentMapper.toDocumentResponse(updatedDoc);
    }

    @Override
    @Transactional
    @PreAuthorize("hasPermission(#docId, 'document', 'documents:delete')")
    @AuditLog(action = "DELETE_DOCUMENT")
    public void deleteDocument(Long docId) {
        Document document = findDocById(docId);

        // Implement soft delete
        document.setStatus(0); // 0 = INACTIVE/DELETED
        documentRepository.save(document);

        log.warn("Document ID {} has been soft-deleted by {}", docId, JwtUtils.getCurrentUserLogin().orElse(""));
        // Lưu ý: File vật lý vẫn còn, có thể viết một scheduled job để xóa file sau một
        // thời gian nhất định.
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
    @PreAuthorize("hasPermission(#docId, 'document', 'documents:approve')")
    @AuditLog(action = "APPROVE_DOCUMENT")
    public DocumentResponse approveDocument(Long docId) {
        Document document = findDocById(docId);

        // ABAC Check: Chỉ phê duyệt khi tài liệu đang ở trạng thái PENDING (giả sử là
        // 2)
        if (document.getStatus() != 2) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "Tài liệu không ở trạng thái chờ phê duyệt.");
        }

        document.setStatus(3); // 3 = APPROVED
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
    @PreAuthorize("hasPermission(#docId, 'document', 'documents:reject')")
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

        document.setStatus(4); // 4 = REJECTED
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
    @PreAuthorize("hasPermission(#docId, 'document', 'documents:share:readonly') or hasPermission(#docId, 'document', 'documents:share:forwardable') or hasPermission(#docId, 'document', 'documents:share:timebound') or hasPermission(#docId, 'document', 'documents:share:orgscope')")
    @AuditLog(action = "SHARE_DOCUMENT")
    public void shareDocument(Long docId, DocumentShareRequest shareRequest) {
        log.info(" nghiệp vụ chia sẻ tài liệu ID: {} với người dùng {}", docId, shareRequest.getRecipientEmail());
        // TODO: Implement logic chia sẻ, tạo bản ghi trong bảng private_docs
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
    @PreAuthorize("hasPermission(#docId, 'document', 'documents:submit')")
    @AuditLog(action = "SUBMIT_DOCUMENT")
    public DocumentResponse submitDocument(Long docId) {
        Document document = findDocById(docId);

        // ABAC Check: Chỉ trình duyệt khi tài liệu đang ở trạng thái DRAFT (giả sử là
        // 1)
        if (document.getStatus() != 1) {
            throw new ApiException(ErrorCode.INVALID_REQUEST,
                    "Chỉ có thể trình duyệt tài liệu ở trạng thái bản nháp (Draft).");
        }

        document.setStatus(2); // 2 = PENDING
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
    @PreAuthorize("hasPermission(#docId, 'document', 'documents:publish')")
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
    public DocumentResponse archiveDocument(Long docId) {
        Document document = findDocById(docId);
        document.setStatus(5); // 5 = ARCHIVED
        Document archivedDoc = documentRepository.save(document);
        log.info("Document ID {} has been archived by {}", docId, JwtUtils.getCurrentUserLogin().orElse(""));
        return documentMapper.toDocumentResponse(archivedDoc);
    }

    @Override
    @Transactional
    @PreAuthorize("hasPermission(#docId, 'document', 'documents:sign')")
    @AuditLog(action = "SIGN_DOCUMENT")
    public DocumentResponse signDocument(Long docId) {
        Document document = findDocById(docId);
        // Trong thực tế, logic này sẽ tích hợp với một dịch vụ chữ ký số.
        // Ở đây, chúng ta chỉ ghi log hành động.
        log.info("Document ID {} has been signed by {}", docId, JwtUtils.getCurrentUserLogin().orElse(""));
        // Có thể thêm một trường isSigned (boolean) vào entity Document nếu cần
        return documentMapper.toDocumentResponse(document);
    }

    @Override
    @Transactional
    @PreAuthorize("hasPermission(#docId, 'document', 'documents:lock')")
    @AuditLog(action = "LOCK_DOCUMENT")
    public DocumentResponse lockDocument(Long docId) {
        Document document = findDocById(docId);
        document.setStatus(6); // 6 = LOCKED
        Document lockedDoc = documentRepository.save(document);
        log.info("Document ID {} has been locked by {}", docId, JwtUtils.getCurrentUserLogin().orElse(""));
        return documentMapper.toDocumentResponse(lockedDoc);
    }

    @Override
    @Transactional
    @PreAuthorize("hasPermission(#docId, 'document', 'documents:unlock')")
    @AuditLog(action = "UNLOCK_DOCUMENT")
    public DocumentResponse unlockDocument(Long docId) {
        Document document = findDocById(docId);
        if (document.getStatus() == 6) { // Chỉ mở khóa khi đang bị khóa
            document.setStatus(3); // Trở về trạng thái APPROVED
        }
        Document unlockedDoc = documentRepository.save(document);
        log.info("Document ID {} has been unlocked by {}", docId, JwtUtils.getCurrentUserLogin().orElse(""));
        return documentMapper.toDocumentResponse(unlockedDoc);
    }

    @Override
    @Transactional
    @PreAuthorize("hasPermission(#docId, 'document', 'documents:comment')")
    @AuditLog(action = "ADD_COMMENT")
    public void addComment(Long docId, DocumentCommentRequest commentRequest) {
        // TODO: Implement logic tạo và lưu entity Comment liên kết với Document và User
        log.info("Thêm bình luận vào tài liệu ID: {}", docId);
    }

    @Override
    @Transactional
    @PreAuthorize("hasPermission(#docId, 'document', 'documents:restore')")
    @AuditLog(action = "RESTORE_DOCUMENT")
    public DocumentResponse restoreDocument(Long docId) {
        Document document = findDocById(docId);
        if (document.getStatus() == 5) { // Chỉ khôi phục khi đang bị lưu trữ
            document.setStatus(3); // Trở về trạng thái APPROVED
        }
        Document restoredDoc = documentRepository.save(document);
        log.info("Document ID {} has been restored by {}", docId, JwtUtils.getCurrentUserLogin().orElse(""));
        return documentMapper.toDocumentResponse(restoredDoc);
    }

    @Override
    @PreAuthorize("hasPermission(#docId, 'document', 'documents:history')")
    @AuditLog(action = "VIEW_HISTORY")
    public List<DocumentVersionResponse> getDocumentVersions(Long docId) {
        // Kiểm tra tài liệu có tồn tại không
        if (!documentRepository.existsById(docId)) {
            throw new ApiException(ErrorCode.DOCUMENT_NOT_FOUND, ErrorMessage.DOCUMENT_NOT_FOUND.getMessage());
        }

        // Lấy tất cả phiên bản và sắp xếp theo version giảm dần
        List<DocumentVersion> versions = documentVersionRepository.findByDocumentIdOrderByVersionNumberDesc(docId);

        log.info("Lấy lịch sử các phiên bản của tài liệu ID: {}", docId);
        return documentVersionMapper.toResponseList(versions);
    }

    @Override
    // Chúng ta sẽ truyền versionNumber vào trong chuỗi permission để
    // CustomPermissionEvaluator xử lý
    @PreAuthorize("hasPermission(#docId, 'document', 'documents:version:read:' + #versionNumber)")
    @AuditLog(action = "VIEW_SPECIFIC_VERSION")
    public DocumentVersionResponse getSpecificDocumentVersion(Long docId, Integer versionNumber) {
        DocumentVersion version = documentVersionRepository.findByDocumentIdAndVersionNumber(docId, versionNumber)
                .orElseThrow(() -> new ApiException(ErrorCode.DOCUMENT_NOT_FOUND, "Phiên bản tài liệu không tồn tại."));

        log.info("Xem phiên bản cụ thể số {} của tài liệu ID: {}", versionNumber, docId);
        return documentVersionMapper.toResponse(version);
    }

    @Override
    @PreAuthorize("hasPermission(#docId, 'document', 'documents:notify')")
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
    @PreAuthorize("hasPermission(#docId, 'document', 'documents:export')")
    @AuditLog(action = "EXPORT_DOCUMENT")
    public ResponseEntity<Resource> exportDocument(Long docId, String format) {
        log.info("Exporting document ID: {} to format: {}", docId, format);
        // Logic này có thể rất phức tạp, ở đây ta giả lập việc export ra file text
        Document document = findDocById(docId);
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
    @PreAuthorize("hasPermission(#docId, 'document', 'documents:forward')")
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
    @PreAuthorize("hasPermission(#docId, 'document', 'documents:distribute')")
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

    // @Override
    // @Transactional
    // @PreAuthorize("hasPermission(#docId, 'document',
    // 'documents:share:external')")
    // @AuditLog(action = "SHARE_DOCUMENT")
    // public String createShareLink(Long docId, Instant expiryAt, boolean
    // allowDownload) {
    // User currentUser =
    // findUserByEmail(JwtUtils.getCurrentUserLogin().orElse(""));
    // Document document = findDocById(docId);

    // // Người dùng phải có quyền truy cập vào tài liệu này mới được chia sẻ
    // authorizeUserCanAccessDocument(currentUser, document);

    // // Tạo token ngẫu nhiên
    // String token = UUID.randomUUID().toString();
    // document.setShareToken(token);
    // document.setPublicShareExpiryAt(expiryAt);
    // document.setAllowPublicDownload(allowDownload);

    // documentRepository.save(document);
    // log.info("User {} created a share link for document ID {}",
    // currentUser.getEmail(), docId);

    // // Trả về URL đầy đủ để hiển thị cho người dùng
    // return baseUrlForSharedDocuments + token;
    // }

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
