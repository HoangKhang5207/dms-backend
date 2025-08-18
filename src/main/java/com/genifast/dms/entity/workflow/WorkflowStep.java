package com.genifast.dms.entity.workflow;

import java.time.Instant;

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
@Table(name = "workflow_steps")
public class WorkflowStep {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "process_key", nullable = false)
    private String processKey;

    @Column(name = "step_order")
    private Integer stepOrder;

    @Column(name = "next_key")
    private String nextKey;

    @Column(name = "task_key", nullable = false)
    private String taskKey;

    @Column(name = "p_condition")
    private String condition;

    @Column(name = "candidate_group")
    private String candidateGroup;

    @Column(name = "reject_key")
    private String rejectKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_id", nullable = false)
    private Workflow workflow;

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
