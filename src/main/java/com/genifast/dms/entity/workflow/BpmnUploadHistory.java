package com.genifast.dms.entity.workflow;

import java.time.Instant;

import com.genifast.dms.entity.Organization;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "bpmn_upload_history")
public class BpmnUploadHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bpmn_upload_id", nullable = false)
    private BpmnUpload bpmnUpload;

    private Integer version;

    private String name;

    @Column(name = "process_key")
    private String processKey;

    private String path;

    @Column(name = "path_svg")
    private String pathSvg;

    @Column(name = "is_published")
    private Boolean isPublished;

    @Column(name = "is_deployed")
    private Boolean isDeployed;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    private Organization organization;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "updated_by")
    private String updatedBy;

    @Column(name = "is_deleted")
    private Boolean isDeleted;
}
