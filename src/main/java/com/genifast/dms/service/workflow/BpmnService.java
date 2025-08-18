package com.genifast.dms.service.workflow;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.genifast.dms.dto.response.workflow.BpmnUploadResponse;

public interface BpmnService {

    BpmnUploadResponse saveBpmn(Long organizationId, String name, MultipartFile bpmnFile, MultipartFile svgFile,
            Long bpmnUploadId, Boolean isPublished);

    List<BpmnUploadResponse> listByOrganization(Long organizationId);

    void softDelete(Long organizationId, Long id);
}
