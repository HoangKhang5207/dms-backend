package com.genifast.dms.mapper.bpmn;

import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import com.genifast.dms.common.utils.AzureSasTokenMapperHelper;
import com.genifast.dms.dto.bpmn.BpmnUploadDTO;
import com.genifast.dms.entity.bpmn.BpmnUpload;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, uses = AzureSasTokenMapperHelper.class)
public interface BpmnUploadMapper {

  @Mapping(source = "path", target = "path", qualifiedByName = "generateSASToken")
  @Mapping(source = "pathSvg", target = "pathSvg", qualifiedByName = "generateSASToken")
  BpmnUploadDTO toDto(BpmnUpload bpmnUpload);

  BpmnUpload toEntity(BpmnUploadDTO bpmnUploadDto);

  List<BpmnUploadDTO> toDtos(List<BpmnUpload> bpmnUploads);
}
