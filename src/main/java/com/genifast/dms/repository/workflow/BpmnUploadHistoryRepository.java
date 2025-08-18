package com.genifast.dms.repository.workflow;

import org.springframework.data.jpa.repository.JpaRepository;

import com.genifast.dms.entity.workflow.BpmnUploadHistory;

public interface BpmnUploadHistoryRepository extends JpaRepository<BpmnUploadHistory, Long> {
}
