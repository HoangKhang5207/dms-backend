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
    
    // Tạo tài liệu mới từ metadata JSON và file upload
    DocumentResponse createDocument(String metadataJson, MultipartFile file);

    // Lấy metadata của tài liệu theo ID
    DocumentResponse getDocumentMetadata(Long id);

    // Tải xuống file tài liệu
    ResponseEntity<Resource> downloadDocumentFile(Long id);

    // Cập nhật metadata của tài liệu
    DocumentResponse updateDocumentMetadata(Long id, DocumentUpdateRequest updateDto);

    // Xóa tài liệu
    void deleteDocument(Long id);

    // Lọc tài liệu theo các tiêu chí
    Page<DocumentResponse> filterDocuments(DocumentFilterRequest filterDto, Pageable pageable);

    // Tìm kiếm tài liệu với điều kiện AND/OR/NOT
    Page<DocumentResponse> searchDocuments(SearchAndOrNotRequest searchDto, Pageable pageable);

    // Phê duyệt tài liệu
    DocumentResponse approveDocument(Long id);

    // Từ chối tài liệu với lý do
    DocumentResponse rejectDocument(Long id, String reason);

    // Chia sẻ tài liệu
    void shareDocument(Long id, DocumentShareRequest shareRequest);

    // Theo dõi lịch sử tài liệu cho audit log
    void trackDocumentHistory(Long id);

    // Gửi tài liệu để xét duyệt
    DocumentResponse submitDocument(Long id);

    // Công khai tài liệu
    DocumentResponse publishDocument(Long id);

    // Lưu trữ tài liệu
    DocumentResponse archiveDocument(Long id);

    // Ký tài liệu
    DocumentResponse signDocument(Long id);

    // Khóa tài liệu
    DocumentResponse lockDocument(Long id);

    // Mở khóa tài liệu
    DocumentResponse unlockDocument(Long id);

    // Thêm bình luận vào tài liệu
    void addComment(Long id, DocumentCommentRequest commentRequest);

    // Khôi phục tài liệu đã bị xóa
    DocumentResponse restoreDocument(Long id);

    // Lấy danh sách các phiên bản của tài liệu
    List<DocumentVersionResponse> getDocumentVersions(Long id);

    // Lấy phiên bản cụ thể của tài liệu
    DocumentVersionResponse getSpecificDocumentVersion(Long id, Integer versionNumber);

    // Gửi thông báo tới người nhận với nội dung message
    void notifyRecipients(Long id, String message);

    // Xuất tài liệu theo định dạng chỉ định
    ResponseEntity<Resource> exportDocument(Long id, String format);

    // Chuyển tiếp tài liệu tới email người nhận
    void forwardDocument(Long id, String recipientEmail);

    // Phân phối tài liệu tới nhiều phòng ban
    void distributeDocument(Long id, List<Long> departmentIds);

    // Tạo báo cáo tài liệu theo loại và phòng ban
    ResponseEntity<Resource> generateDocumentReport(String reportType, Long departmentId);

    // Tạo link chia sẻ tài liệu với thời gian hết hạn và quyền tải xuống
    String createShareLink(Long id, Instant expiryAt, boolean allowDownload);
}