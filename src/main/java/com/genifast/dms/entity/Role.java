package com.genifast.dms.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "roles", uniqueConstraints = {
        // Đảm bảo tên Role là duy nhất trong phạm vi một tổ chức.
        // Một tổ chức không thể có 2 role trùng tên.
        @UniqueConstraint(columnNames = { "name", "organization_id" })
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id") // Ánh xạ tới cột organization_id
    private Organization organization; // Có thể NULL cho các role toàn hệ thống (SYSTEM_ADMIN)

    @Column(name = "name", nullable = false, length = 50)
    private String name; // e.g., "ORGANIZATION_MANAGER", "CONTENT_EDITOR"

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_inheritable", nullable = false)
    @Builder.Default
    private Boolean isInheritable = false;

    @ManyToMany(fetch = FetchType.EAGER) // EAGER để khi load Role sẽ có ngay danh sách Permission
    @JoinTable(name = "role_permissions", joinColumns = @JoinColumn(name = "role_id"), inverseJoinColumns = @JoinColumn(name = "permission_id"))
    @Builder.Default
    private Set<Permission> permissions = new HashSet<>();

    @ManyToMany(mappedBy = "roles")
    private Set<User> users;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}