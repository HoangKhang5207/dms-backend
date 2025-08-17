package com.genifast.dms.entity.workflow;

import com.genifast.dms.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "workflow_steps")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowSteps extends BaseEntity {
    @Column(name = "process_key", nullable = false)
    private String processKey;

    @Column(name = "step_order")
    private int stepOrder;

    @Column(name = "next_key")
    private String nextKey;

    @Column(name = "task_key")
    private String taskKey;

    @Column(name = "p_condition")
    private String condition;

    @Column(name = "candidate_group")
    private String candidateGroup;

    @Column(name = "reject_key")
    private String rejectKey;

    @ManyToOne
    @JoinColumn(name = "workflow_id", nullable = false)
    private Workflow workflow;
}
