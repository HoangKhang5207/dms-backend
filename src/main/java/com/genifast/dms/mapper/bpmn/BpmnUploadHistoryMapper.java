package com.genifast.dms.mapper.bpmn;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import com.genifast.dms.common.utils.LocalStorageMapperHelper;
import com.genifast.dms.dto.bpmn.BpmnUploadHistoryDTO;
import com.genifast.dms.dto.bpmn.VersionInfoDTO;
import com.genifast.dms.entity.bpmn.BpmnUpload;
import com.genifast.dms.entity.bpmn.BpmnUploadHistory;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, uses = LocalStorageMapperHelper.class)
public interface BpmnUploadHistoryMapper {
  @Mapping(source = "path", target = "path", qualifiedByName = "generateSASToken")
  @Mapping(source = "pathSvg", target = "pathSvg", qualifiedByName = "generateSASToken")
  @Mapping(source = "organization.id", target = "organizationId")
  BpmnUploadHistoryDTO toDto(BpmnUploadHistory bpmnUploadHistory);

  VersionInfoDTO toVersionInfo(BpmnUploadHistory bpmnUploadHistory);

  @Mapping(target = "organization", ignore = true)
  BpmnUploadHistory toEntity(BpmnUploadHistoryDTO bpmnUploadHistoryDto);

  /**
   * Chuyển đổi một đối tượng BpmnUpload thành một đối tượng BpmnUploadHistory.
   * 
   * @param bpmnUpload đối tượng nguồn
   * @return đối tượng BpmnUploadHistory
   */
  @Mapping(target = "id", ignore = true) // Bỏ qua ID để CSDL tự tạo
  @Mapping(source = "id", target = "bpmnUpload.id") // Liên kết history với BpmnUpload gốc
  BpmnUploadHistory fromBpmnUpload(BpmnUpload bpmnUpload);
}
