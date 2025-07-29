package com.genifast.dms.entity;

import java.time.Instant;

import com.genifast.dms.entity.enums.CommonStatus;
import com.genifast.dms.entity.enums.UserDocType;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "user_documents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDocument {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @Column(name = "type")
    private Integer type;

    @Column(name = "status")
    private Integer status;

    @Column(name = "decentralized_by", length = 30)
    private String decentralizedBy;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "viewed_at")
    private Instant viewedAt;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "move_to_trash_at")
    private Instant moveToTrashAt;

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
