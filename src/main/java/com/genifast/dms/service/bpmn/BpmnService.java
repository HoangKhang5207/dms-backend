package com.genifast.dms.service.bpmn;

import java.util.List;

import com.genifast.dms.dto.bpmn.BpmnUploadDTO;
import com.genifast.dms.dto.bpmn.BpmnUploadHistoryDTO;
import com.genifast.dms.dto.bpmn.VersionInfoDTO;
import com.genifast.dms.dto.bpmn.request.SaveBRequest;
import com.genifast.dms.entity.bpmn.BpmnUpload;
import com.genifast.dms.service.BaseService;

public interface BpmnService extends BaseService<BpmnUpload, Long> {
    List<BpmnUploadDTO> getBpmnList(Long organizationId);

    BpmnUploadHistoryDTO getBpmnInfo(Long bpmnUploadId, int version);

    List<BpmnUploadDTO> getPublishedBpmnList(Long organizationId);

    List<VersionInfoDTO> getVersionInfo(Long bpmnUploadId);

    BpmnUpload getBpmnUpload(Long id);

    BpmnUploadDTO saveBpmnUpload(SaveBRequest saveBRequest, Long organizationId);

    void deleteBpmn(Long bpmnUploadId);
}
