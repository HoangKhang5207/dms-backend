package com.genifast.dms.entity.bpmn;

import com.genifast.dms.entity.BaseEntity;
import com.genifast.dms.entity.Organization;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "bpmn_upload_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BpmnUploadHistory extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bpmn_upload_id", nullable = false)
    private BpmnUpload bpmnUpload;

    @Column(name = "version", nullable = false)
    private int version;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "process_key")
    private String processKey;

    @Column(name = "path")
    private String path;

    @Column(name = "path_svg")
    private String pathSvg;

    @Column(name = "is_published")
    private Boolean isPublished = false;

    @Column(name = "is_deployed")
    private Boolean isDeployed = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    private Organization organization;
}
