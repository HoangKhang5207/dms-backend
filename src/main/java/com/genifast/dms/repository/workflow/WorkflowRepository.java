package com.genifast.dms.repository.workflow;

import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.genifast.dms.entity.workflow.Workflow;
import com.genifast.dms.repository.BaseRepository;
import com.genifast.dms.repository.workflow.workflow_custom.WorkflowCRepository;

@Repository
public interface WorkflowRepository extends BaseRepository<Workflow, Long>, WorkflowCRepository {
  @Query("""
        SELECT DISTINCT w FROM Workflow w
            LEFT JOIN FETCH w.bpmnUpload
            LEFT JOIN FETCH w.workflowEle
            LEFT JOIN FETCH w.workflowDepts
            WHERE w.bpmnUpload.organization.id = :organizationId
      """)
  List<Workflow> findByOrganizationId(Long organizationId);

  @Query("SELECT w FROM Workflow w JOIN FETCH w.bpmnUpload bu WHERE w.id = :workflowId")
  Workflow findByWorkflowId(Long workflowId);
}
