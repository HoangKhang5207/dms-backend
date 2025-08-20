package com.genifast.dms.entity;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import com.genifast.dms.common.utils.JwtUtils;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "documents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Document {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(name = "status")
    private Integer status;

    @Column(name = "created_by", length = 255)
    private String createdBy;

    @Column(length = 30)
    private String type;

    @Column(name = "total_page")
    private Integer totalPage;

    @Column(length = 255)
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "file_path", length = 255)
    private String filePath;

    @Column(name = "file_id", length = 255)
    private String fileId;

    @Column(name = "storage_capacity")
    private Long storageCapacity;

    @Column(name = "storage_unit", length = 255)
    private String storageUnit;

    @Column(name = "original_filename")
    private String originalFilename;

    @Column(name = "content_type")
    private String contentType;

    @Column(name = "access_type")
    private Integer accessType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dept_id")
    private Department department;

    @Column(name = "title_unaccent", columnDefinition = "TEXT")
    private String titleUnaccent;

    @Column(name = "password", columnDefinition = "TEXT")
    private String password;

    @Column(name = "photo_id", columnDefinition = "TEXT")
    private String photoId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id") // Trường này có thể là NULL
    private Project project;

    // --- BỔ SUNG CÁC TRƯỜNG MỚI CHO VISITOR ---
    @Column(name = "share_token", length = 36, unique = true)
    private String shareToken; // UUID để tạo link chia sẻ

    @Column(name = "public_share_expiry_at")
    private Instant publicShareExpiryAt; // Thời gian hết hạn của link

    @Column(name = "allow_public_download")
    @Builder.Default
    private boolean allowPublicDownload = false; // Mặc định không cho phép tải

    @Column(name = "archived_at")
    private Instant archivedAt; // thời điểm lưu trữ

    @Column(name = "confidentiality")
    @Builder.Default
    private Integer confidentiality = 2; // Default: INTERNAL

    @Column(name = "recipients", columnDefinition = "TEXT")
    private String recipients; // JSON array chứa danh sách user IDs

    @Column(name = "version_number")
    @Builder.Default
    private Integer versionNumber = 1;

    @Column(name = "signed_at")
    private Instant signedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "signed_by")
    private User signedBy;

    @OneToMany(mappedBy = "document", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<PrivateDoc> privateDocuments = new HashSet<>();

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        createdBy = JwtUtils.getCurrentUserLogin().orElse("");
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    // Helper methods for ABAC
    public String getRecipients() {
        return this.recipients;
    }

    public Integer getConfidentiality() {
        return this.confidentiality;
    }

    public Long getOrganizationId() {
        return this.organization != null ? this.organization.getId() : null;
    }

    public Long getDeptId() {
        return this.department != null ? this.department.getId() : null;
    }

    public Long getProjectId() {
        return this.project != null ? this.project.getId() : null;
    }

}
