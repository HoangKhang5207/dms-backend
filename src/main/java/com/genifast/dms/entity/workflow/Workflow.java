package com.genifast.dms.entity.workflow;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

import com.genifast.dms.entity.BaseEntity;
import com.genifast.dms.entity.Organization;
import com.genifast.dms.entity.bpmn.BpmnUpload;
import com.genifast.dms.entity.enums.DocumentType;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "workflow")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Workflow extends BaseEntity {

    @Column(name = "version")
    private int version = 0;

    @Column(name = "document_type", nullable = false)
    private DocumentType documentType;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "started_role", nullable = false)
    private String startedRole;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    private Organization organization;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bpmn_upload_id", nullable = false)
    private BpmnUpload bpmnUpload;

    @OneToOne(mappedBy = "workflow")
    private WorkflowEle workflowEle;

    @OneToMany(mappedBy = "workflow")
    private List<WorkflowSteps> workflowSteps;

    @OneToMany(mappedBy = "workflow", fetch = FetchType.LAZY)
    private List<WorkflowDept> workflowDepts = new ArrayList<>();
}
