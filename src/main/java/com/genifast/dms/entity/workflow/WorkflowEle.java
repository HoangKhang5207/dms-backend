package com.genifast.dms.entity.workflow;

import com.genifast.dms.entity.BaseEntity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "workflow_ele")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowEle extends BaseEntity {

    @Column(name = "category_ids")
    private String categoryIds;

    @Column(name = "urgency")
    private String urgency;

    @Column(name = "security")
    private String security;

    @OneToOne
    @JoinColumn(name = "workflow_id")
    private Workflow workflow;

    public WorkflowEle(String categoryIds, String urgency, String security) {
        this.categoryIds = categoryIds;
        this.urgency = urgency;
        this.security = security;
    }
}
