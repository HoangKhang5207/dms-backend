package com.genifast.dms.entity;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import com.genifast.dms.common.utils.JwtUtils;
import com.genifast.dms.entity.enums.DocumentType;

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

    // BỔ SUNG: Thêm trường để lưu số hiệu của phiên bản mới nhất
    @Column(name = "latest_version", nullable = false)
    @Builder.Default
    private Integer latestVersion = 1;

    @Enumerated(EnumType.STRING) // Lưu tên của Enum vào DB (e.g., "OUTGOING")
    @Column(name = "document_type")
    private DocumentType documentType;

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

    @Column(name = "password", length = 255)
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
    private boolean allowPublicDownload = false; // Mặc định không cho phép tải

    // BỔ SUNG: Quan hệ một-nhiều tới các phiên bản
    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private Set<DocumentVersion> versions = new HashSet<>();

    @OneToMany(mappedBy = "document", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<PrivateDoc> privateDocuments = new HashSet<>();

    // BỔ SUNG: Thêm quan hệ Many-to-Many để quản lý người nhận (recipients)
    // Điều này sẽ tạo ra một bảng trung gian là `document_recipients`
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "document_recipients", joinColumns = @JoinColumn(name = "document_id"), inverseJoinColumns = @JoinColumn(name = "user_id"))
    @Builder.Default
    private Set<User> recipients = new HashSet<>();

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

}
