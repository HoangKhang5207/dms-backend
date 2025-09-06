package com.genifast.dms.entity.workflow;

import com.genifast.dms.entity.BaseEntity;
import com.genifast.dms.entity.enums.DocumentType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "workflow_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowHistory extends BaseEntity {

    @Column(name = "workflow_id")
    private Long workflowId;

    @Column(name = "version")
    private int version = 0;

    @Column(name = "document_type")
    private DocumentType documentType;

    @Column(name = "name")
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "started_role")
    private String startedRole;
}
