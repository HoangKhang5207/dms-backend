package com.genifast.dms.repository.workflow;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.genifast.dms.entity.workflow.DocumentWorkflowInstance;

public interface DocumentWorkflowInstanceRepository extends JpaRepository<DocumentWorkflowInstance, Long> {
    Optional<DocumentWorkflowInstance> findByProcessInstanceId(String processInstanceId);
    List<DocumentWorkflowInstance> findByDocument_Id(Long documentId);
}
