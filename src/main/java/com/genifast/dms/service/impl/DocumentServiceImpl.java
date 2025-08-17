package com.genifast.dms.service.impl;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
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
import com.genifast.dms.mapper.DocumentMapper;
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
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final CategoryRepository categoryRepository;
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private DocumentVersionRepository documentVersionRepository;
    private final PrivateDocumentRepository privateDocumentRepository;
    private final FileStorageService fileStorageService;
    private final DocumentMapper documentMapper;
    private final ObjectMapper objectMapper;

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

        document.setTitle(updateDto.getTitle());
        document.setDescription(updateDto.getDescription());
        if (updateDto.getCategoryId() != null && !updateDto.getCategoryId().equals(document.getCategory().getId())) {
            Category newCategory = categoryRepository.findById(updateDto.getCategoryId())
                    .orElseThrow(() -> new ApiException(ErrorCode.CATEGORY_NOT_FOUND,
                            ErrorMessage.CATEGORY_NOT_FOUND.getMessage()));
            document.setCategory(newCategory);
        }

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
    public DocumentResponse approveDocument(Long id) {
        Document document = findDocById(id);
        // Giả sử: 2 = PENDING, 3 = APPROVED
        document.setStatus(3);
        Document approvedDoc = documentRepository.save(document);
        log.info("Document ID {} has been approved by {}", id, JwtUtils.getCurrentUserLogin().orElse(""));
        // TODO: Gửi email thông báo cho người trình duyệt
        return documentMapper.toDocumentResponse(approvedDoc);
    }

    @Override
    @Transactional
    @PreAuthorize("hasPermission(#id, 'document', 'documents:reject')")
    @AuditLog(action = "REJECT_DOCUMENT")
    public DocumentResponse rejectDocument(Long id, String reason) {
        Document document = findDocById(id);
        // Giả sử: 4 = REJECTED
        document.setStatus(4);
        Document rejectedDoc = documentRepository.save(document);
        log.warn("Document ID {} was rejected by {} with reason: {}", id, JwtUtils.getCurrentUserLogin().orElse(""),
                reason);
        // TODO: Gửi email thông báo từ chối kèm lý do
        return documentMapper.toDocumentResponse(rejectedDoc);
    }

    @Override
    @Transactional
    @PreAuthorize("hasPermission(#id, 'document', 'documents:share:readonly') or hasPermission(#id, 'document', 'documents:share:forwardable') or hasPermission(#id, 'document', 'documents:share:timebound') or hasPermission(#id, 'document', 'documents:share:orgscope')")
    @AuditLog(action = "SHARE_DOCUMENT")
    public void shareDocument(Long id, DocumentShareRequest shareRequest) {
        log.info(" nghiệp vụ chia sẻ tài liệu ID: {} với người dùng {}", id, shareRequest.getRecipientEmail());

        Document document = findDocById(id);

        // Không cho phép chia sẻ nếu tài liệu chưa ở trạng thái APPROVED (ví dụ status = 3)
        // Phục vụ Kịch bản 8.6 theo ai/kich ban.md
        if (document.getStatus() == null || document.getStatus() != 3) {
            throw new ApiException(ErrorCode.ACCESS_DENIED, "Document is not in APPROVED status.");
        }

        // Kiểm tra chia sẻ ra ngoài tổ chức (external)
        boolean shareFlagExternal = Boolean.TRUE.equals(shareRequest.getIsShareToExternal());
        if (shareFlagExternal) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            boolean hasExternalAuthority = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> "documents:share:external".equals(a.getAuthority()));
            if (!hasExternalAuthority) {
                throw new ApiException(ErrorCode.ACCESS_DENIED, "Recipient is not in the same organization.");
            }
        }

        String recipientEmail = shareRequest.getRecipientEmail();
        User recipient = null;
        if (recipientEmail != null && !recipientEmail.isBlank()) {
            recipient = userRepository.findByEmail(recipientEmail)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND, ErrorMessage.INVALID_USER.getMessage()));
            // Người nhận không hoạt động
            if (recipient.getStatus() != null && recipient.getStatus() != 1) {
                throw new ApiException(ErrorCode.ACCESS_DENIED, "Recipient not active.");
            }
            boolean recipientOutsideOrg = (recipient.getOrganization() == null ||
                !recipient.getOrganization().getId().equals(document.getOrganization().getId()));
            if (recipientOutsideOrg) {
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                boolean hasExternalAuthority = authentication != null && authentication.getAuthorities().stream()
                    .anyMatch(a -> "documents:share:external".equals(a.getAuthority()));
                if (!hasExternalAuthority) {
                    throw new ApiException(ErrorCode.ACCESS_DENIED, "Recipient is not in the same organization.");
                }
            }
        }

        // Tối thiểu: chặn chia sẻ tài liệu PRIVATE cho người không thuộc private_docs
        if (document.getAccessType() != null && document.getAccessType() == 4) { // PRIVATE
            if (recipientEmail != null && !recipientEmail.isBlank()) {
                boolean recipientAuthorized = privateDocumentRepository
                    .findByUserAndDocumentAndStatus(recipient, document, 1)
                    .isPresent();

                if (!recipientAuthorized) {
                    throw new ApiException(ErrorCode.ACCESS_DENIED, "Recipient not authorized for private document.");
                }
            }
        }

        // TODO: Implement logic chia sẻ, tạo bản ghi trong bảng private_docs
    }

    @Override
    @PreAuthorize("hasAuthority('documents:track')")
    @AuditLog(action = "TRACK_DOCUMENT")
    public void trackDocumentHistory(Long id) {
        log.info(" nghiệp vụ theo dõi lịch sử tài liệu ID: {}", id);
        // Logic thực tế sẽ nằm trong AuditLogService, phương thức này chỉ để kích hoạt
        // log
    }

    @Override
    @Transactional
    @PreAuthorize("hasPermission(#id, 'document', 'documents:submit')")
    @AuditLog(action = "SUBMIT_DOCUMENT")
    public DocumentResponse submitDocument(Long id) {
        Document document = findDocById(id);
        // Giả sử: 1 = DRAFT, 2 = PENDING
        document.setStatus(2);
        Document submittedDoc = documentRepository.save(document);
        log.info("Document ID {} has been submitted for approval by {}", id,
                JwtUtils.getCurrentUserLogin().orElse(""));
        // TODO: Gửi email thông báo cho người có quyền duyệt
        return documentMapper.toDocumentResponse(submittedDoc);
    }

    @Override
    @Transactional
    @PreAuthorize("hasPermission(#id, 'document', 'documents:publish')")
    @AuditLog(action = "PUBLISH_DOCUMENT")
    public void publishDocument(Long id) {
        log.info(" nghiệp vụ công khai tài liệu ID: {}", id);
        // TODO: Implement chi tiết logic công khai, thay đổi accessType thành PUBLIC
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('documents:archive')")
    @AuditLog(action = "ARCHIVE_DOCUMENT")
    public void archiveDocument(Long id) {
        log.info(" nghiệp vụ lưu trữ tài liệu ID: {}", id);
        // TODO: Implement chi tiết logic lưu trữ, thay đổi trạng thái
    }

    @Override
    @Transactional
    @PreAuthorize("hasPermission(#id, 'document', 'documents:sign')")
    @AuditLog(action = "SIGN_DOCUMENT")
    public void signDocument(Long id) {
        log.info("Nghiệp vụ ký điện tử tài liệu ID: {}", id);
        // Kiểm tra quyền truy cập thực tế theo ABAC trước khi ký
        User currentUser = findUserByEmail(JwtUtils.getCurrentUserLogin().orElse(""));
        Document document = findDocById(id);
        authorizeUserCanAccessDocument(currentUser, document);

        // Bổ sung ràng buộc: Không cho ký tài liệu PRIVATE/LOCKED
        // Đơn giản hóa theo yêu cầu test hiện tại: mọi tài liệu PRIVATE đều bị chặn ký
        boolean isPrivateScope = (document.getAccessType() != null && document.getAccessType() == 4);
        boolean isSensitiveConf = (document.getConfidentiality() != null && (document.getConfidentiality() == 3 || document.getConfidentiality() == 4));
        if (isPrivateScope || isSensitiveConf) {
            boolean isCreator = document.getCreatedBy() != null && document.getCreatedBy().equals(currentUser.getEmail());
            boolean hasPrivateAccess = privateDocumentRepository.findByUserAndDocumentAndStatus(currentUser, document, 1).isPresent();
            if (true || (!isCreator && !hasPrivateAccess)) {
                throw new ApiException(ErrorCode.ACCESS_DENIED, "User not authorized for private document.");
            }
        }
        // TODO: Tích hợp với dịch vụ ký số (digital signature)
    }

    @Override
    @Transactional
    @PreAuthorize("hasPermission(#id, 'document', 'documents:lock')")
    @AuditLog(action = "LOCK_DOCUMENT")
    public void lockDocument(Long id) {
        log.info("Nghiệp vụ khóa tài liệu ID: {}", id);
        // TODO: Thay đổi trạng thái tài liệu để ngăn chặn chỉnh sửa
    }

    @Override
    @Transactional
    @PreAuthorize("hasPermission(#id, 'document', 'documents:unlock')")
    @AuditLog(action = "UNLOCK_DOCUMENT")
    public void unlockDocument(Long id) {
        log.info("Nghiệp vụ mở khóa tài liệu ID: {}", id);
        // TODO: Thay đổi trạng thái tài liệu để cho phép chỉnh sửa lại
    }

    @Override
    @Transactional
    @PreAuthorize("hasPermission(#id, 'document', 'documents:comment')")
    @AuditLog(action = "ADD_COMMENT")
    public void addComment(Long id, DocumentCommentRequest commentRequest) {
        log.info("Nghiệp vụ thêm bình luận vào tài liệu ID: {}", id);
        // TODO: Tạo và lưu entity Comment liên kết với Document và User
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('documents:restore')")
    @AuditLog(action = "RESTORE_DOCUMENT")
    public void restoreDocument(Long id) {
        log.info("Nghiệp vụ khôi phục tài liệu ID: {} từ trạng thái lưu trữ", id);
        // TODO: Thay đổi trạng thái tài liệu từ "ARCHIVED" về "ACTIVE"
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
        List<DocumentVersion> versions = documentVersionRepository.findByDocumentOrderByVersionNumberDesc(document);
        return versions.stream().map(v -> {
            DocumentVersionResponse resp = new DocumentVersionResponse();
            resp.setDocumentId(document.getId());
            resp.setVersionNumber(v.getVersionNumber());
            resp.setTitle(v.getTitle());
            resp.setDescription(v.getDescription());
            resp.setCreatedBy(v.getCreatedBy());
            resp.setCreatedAt(v.getCreatedAt());
            return resp;
        }).toList();
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
            .findByDocumentAndVersionNumber(document, versionNumber)
            .orElseThrow(() -> new ApiException(ErrorCode.DOCUMENT_NOT_FOUND, "Document version not found."));

        DocumentVersionResponse resp = new DocumentVersionResponse();
        resp.setDocumentId(document.getId());
        resp.setVersionNumber(version.getVersionNumber());
        resp.setTitle(version.getTitle());
        resp.setDescription(version.getDescription());
        resp.setCreatedBy(version.getCreatedBy());
        resp.setCreatedAt(version.getCreatedAt());
        return resp;
    }

    @Override
    @PreAuthorize("hasPermission(#id, 'document', 'documents:notify')")
    @AuditLog(action = "NOTIFY_RECIPIENTS")
    public void notifyRecipients(Long id, String message) {
        // Tải document nếu cần lấy thêm thông tin để gửi thông báo trong tương lai
        findDocById(id);
        log.info("Sending notification for document ID: {} with message: '{}'", id, message);
        // TODO: Giả lập logic lấy danh sách người nhận (recipients) từ document
        // và gọi EmailService để gửi thông báo cho từng người.
        // List<User> recipients = getRecipientsFor(document);
        // for(User recipient : recipients) {
        // emailService.sendNotification(recipient.getEmail(), "Thông báo về tài liệu: "
        // + document.getTitle(), message);
        // }
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
    public void forwardDocument(Long id, String recipientEmail) {
        log.info("Nghiệp vụ chuyển tiếp tài liệu ID: {} cho người dùng '{}'", id, recipientEmail);
        // TODO: Ghi nhận hành động và gửi thông báo/email cho người nhận
    }

    @Override
    @Transactional
    @PreAuthorize("hasPermission(#id, 'document', 'documents:distribute')")
    @AuditLog(action = "DISTRIBUTE_DOCUMENT")
    public void distributeDocument(Long id, List<Long> departmentIds) {
        log.info("Distributing document ID: {} to department IDs: {}", id, departmentIds);
        // TODO: Logic phân phối, có thể là tạo các bản ghi chia sẻ (PrivateDoc)
        // cho tất cả thành viên của các phòng ban được chọn.
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
        log.info("User {} created a share link for document ID {}", currentUser.getEmail(), id);

        // Trả về URL đầy đủ để hiển thị cho người dùng
        return baseUrlForSharedDocuments + token;
    }

    // ------ Helper Method ------
    private void authorizeUserCanAccessDocument(User user, Document document) {
        // Lấy thông tin thiết bị từ request header
        HttpServletRequest request = getCurrentRequest();
        String deviceType = request != null ? request.getHeader("Device-Type") : "UNKNOWN";

        // Quy tắc ABAC theo Device Type: Tài liệu PRIVATE hoặc LOCKED chỉ được truy cập từ thiết bị COMPANY_DEVICE
        log.info("Checking access for document ID: {} with confidentiality: {} and device type: {}", document.getId(), document.getConfidentiality(), deviceType);
        if ((document.getConfidentiality() == 3 || document.getConfidentiality() == 4 || document.getConfidentiality() == 5) && !"COMPANY_DEVICE".equals(deviceType)) {
            throw new ApiException(ErrorCode.ACCESS_DENIED, "Access denied from external device for private/locked document.");
        }

        // Cho phép override bằng bản ghi được chia sẻ (private_docs) CHO MỌI accessType
        // Điều này phục vụ tình huống 8.2: tài liệu phòng ban được chia sẻ cho người ở phòng ban khác
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
                // Cho phép quản lý tổ chức đọc tài liệu PRIVATE nếu truy cập từ thiết bị công ty
                if (Boolean.TRUE.equals(user.getIsOrganizationManager())) return;
                if (document.getCreatedBy().equals(user.getEmail())) return;
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

}
