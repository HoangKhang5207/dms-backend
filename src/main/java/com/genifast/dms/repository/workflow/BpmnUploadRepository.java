package com.genifast.dms.repository.workflow;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.genifast.dms.entity.workflow.BpmnUpload;

public interface BpmnUploadRepository extends JpaRepository<BpmnUpload, Long> {
    List<BpmnUpload> findByOrganization_IdAndIsDeletedFalse(Long organizationId);
    Optional<BpmnUpload> findByIdAndOrganization_Id(Long id, Long organizationId);
}
