package com.genifast.dms.repository.bpmn;

import java.util.List;
import org.springframework.stereotype.Repository;

import com.genifast.dms.entity.bpmn.BpmnUpload;
import com.genifast.dms.repository.BaseRepository;

@Repository
public interface BpmnUploadRepository extends BaseRepository<BpmnUpload, Long> {
    List<BpmnUpload> findByOrganizationIdAndIsDeletedFalse(Long organizationId);

    List<BpmnUpload> findByOrganizationIdAndIsPublishedTrueAndIsDeletedFalse(Long organizationId);
}
