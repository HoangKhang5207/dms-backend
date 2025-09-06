package com.genifast.dms.repository.workflow.workflow_custom;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import java.util.ArrayList;
import java.util.List;

import com.genifast.dms.dto.workflow.AssignedWorkflow;
import com.genifast.dms.dto.workflow.WorkflowStepsDTO;
import com.genifast.dms.entity.bpmn.BpmnUpload;
import com.genifast.dms.entity.workflow.Workflow;
import com.genifast.dms.entity.workflow.WorkflowDept;
import com.genifast.dms.entity.workflow.WorkflowEle;
import com.genifast.dms.entity.workflow.WorkflowSteps;

public class WorkflowCRepositoryImpl implements WorkflowCRepository {
  @PersistenceContext
  private EntityManager em;

  @Override
  public List<Workflow> getAssignedWorkflows(String role, Long departmentId) {
    CriteriaBuilder cb = em.getCriteriaBuilder();
    CriteriaQuery<Workflow> cq = cb.createQuery(Workflow.class);
    Root<WorkflowDept> workflowDeptRoot = cq.from(WorkflowDept.class);
    Join<WorkflowDept, Workflow> workflowJoin = workflowDeptRoot.join("workflow");

    cq.select(workflowJoin)
        .where(
            cb.equal(workflowDeptRoot.get("department").get("id"), departmentId),
            cb.equal(workflowJoin.get("startedRole"), role));

    TypedQuery<Workflow> query = em.createQuery(cq);
    return query.getResultList();
  }

  @Override
  public List<AssignedWorkflow> getAssignedWorkflow(Long workflowId) {
    CriteriaBuilder cb = em.getCriteriaBuilder();
    CriteriaQuery<Object[]> cq = cb.createQuery(Object[].class);

    Root<Workflow> workflowRoot = cq.from(Workflow.class);
    Join<Workflow, BpmnUpload> bpmnUploadJoin = workflowRoot.join("bpmnUpload");
    Join<Workflow, WorkflowSteps> workflowStepsJoin = workflowRoot.join("workflowSteps", JoinType.LEFT);
    Join<Workflow, WorkflowEle> workflowEleJoin = workflowRoot.join("workflowEle", JoinType.LEFT);

    cq.multiselect(
        workflowRoot.get("id"),
        workflowRoot.get("name"),
        workflowEleJoin.get("categoryIds"),
        workflowEleJoin.get("urgency"),
        workflowEleJoin.get("security"),
        bpmnUploadJoin.get("id"),
        bpmnUploadJoin.get("path"),
        bpmnUploadJoin.get("pathSvg"),
        workflowStepsJoin.get("id"),
        workflowStepsJoin.get("stepOrder"),
        workflowStepsJoin.get("nextKey"),
        workflowStepsJoin.get("condition"),
        workflowStepsJoin.get("candidateGroup")).where(
            cb.equal(workflowRoot.get("id"), workflowId),
            cb.equal(workflowStepsJoin.get("stepOrder"), 1));

    TypedQuery<Object[]> query = em.createQuery(cq);
    List<Object[]> results = query.getResultList();
    List<AssignedWorkflow> assignedWorkflows = new ArrayList<>();

    for (Object[] row : results) {
      AssignedWorkflow aw = new AssignedWorkflow();
      aw.setId((Long) row[0]);
      aw.setName((String) row[1]);
      aw.setCategoryIds((String) row[2]);
      aw.setUrgency((String) row[3]);
      aw.setSecurity((String) row[4]);

      BpmnUpload bpmnUpload = new BpmnUpload();
      bpmnUpload.setId((Long) row[5]);
      bpmnUpload.setPath((String) row[6]);
      bpmnUpload.setPathSvg((String) row[7]);
      aw.setBpmnUpload(bpmnUpload);

      WorkflowStepsDTO workflowStepsDto = new WorkflowStepsDTO();
      workflowStepsDto.setId((Long) row[8]);
      workflowStepsDto.setStepOrder(row[9] != null ? ((Number) row[9]).intValue() : null);
      workflowStepsDto.setNextKey((String) row[10]);
      workflowStepsDto.setCondition((String) row[11]);
      workflowStepsDto.setCandidateGroup((String) row[12]);
      aw.setWorkflowStepsDto(workflowStepsDto);

      assignedWorkflows.add(aw);
    }

    return assignedWorkflows.isEmpty() ? null : assignedWorkflows;
  }
}