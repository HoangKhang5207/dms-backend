package com.genifast.dms.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "delegations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Delegation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delegator_id", nullable = false)
    private User delegator; // Người ủy quyền

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delegatee_id", nullable = false)
    private User delegatee; // Người được ủy quyền

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @Column(name = "permission", nullable = false, length = 50)
    private String permission; // Quyền được ủy quyền

    @Column(name = "expiry_at")
    private Instant expiryAt; // Thời điểm hết hạn. NULL nếu vô thời hạn.

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}