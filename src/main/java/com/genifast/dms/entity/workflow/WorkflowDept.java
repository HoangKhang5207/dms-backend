package com.genifast.dms.entity.workflow;

import com.genifast.dms.entity.BaseEntity;
import com.genifast.dms.entity.Department;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "workflow_org")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowDept extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_id")
    private Workflow workflow;
}
