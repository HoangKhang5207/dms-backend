package com.genifast.dms.entity;

import jakarta.persistence.*;
import java.time.Instant;
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

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "document_id", nullable = false)
    private Long documentId;

    private Integer type;

    @Column(nullable = false)
    private Integer status;

    @Column(name = "decentralized_by")
    private String decentralizedBy;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "viewed_at")
    private Instant viewedAt;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "move_to_trash_at")
    private Instant moveToTrashAt;

}
