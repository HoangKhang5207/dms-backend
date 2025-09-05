package com.genifast.dms.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.Instant;

import com.genifast.dms.common.utils.JwtUtils;

@Entity
@Table(name = "document_versions", indexes = {
        @Index(name = "idx_doc_version_number", columnList = "document_id, version_number", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Quan hệ nhiều-một với tài liệu gốc
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @Column(name = "version_number", nullable = false)
    private Integer versionNumber;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    // Lưu lại trạng thái của tài liệu tại thời điểm tạo phiên bản
    @Column(name = "status", nullable = false)
    private Integer status;

    // Ghi chú cho sự thay đổi ở phiên bản này
    @Column(name = "change_description", columnDefinition = "TEXT")
    private String changeDescription;

    // Đường dẫn tới file vật lý của phiên bản này
    @Column(name = "file_path", nullable = false)
    private String filePath;

    @Column(name = "created_by", updatable = false)
    private String createdBy;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        createdBy = JwtUtils.getCurrentUserLogin().orElse("");
    }

}