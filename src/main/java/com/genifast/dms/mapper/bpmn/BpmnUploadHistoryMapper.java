package com.genifast.dms.mapper.bpmn;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import com.genifast.dms.common.utils.AzureSasTokenMapperHelper;
import com.genifast.dms.dto.bpmn.BpmnUploadHistoryDTO;
import com.genifast.dms.dto.bpmn.VersionInfoDTO;
import com.genifast.dms.entity.bpmn.BpmnUploadHistory;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, uses = AzureSasTokenMapperHelper.class)
public interface BpmnUploadHistoryMapper {
  @Mapping(source = "path", target = "path", qualifiedByName = "generateSASToken")
  @Mapping(source = "pathSvg", target = "pathSvg", qualifiedByName = "generateSASToken")
  BpmnUploadHistoryDTO toDto(BpmnUploadHistory bpmnUploadHistory);

  VersionInfoDTO toVersionInfo(BpmnUploadHistory bpmnUploadHistory);

  BpmnUploadHistory toEntity(BpmnUploadHistoryDTO bpmnUploadHistoryDto);
}
