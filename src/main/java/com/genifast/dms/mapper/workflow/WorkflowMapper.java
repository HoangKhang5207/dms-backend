package com.genifast.dms.mapper.workflow;

import java.util.List;
import java.util.stream.Collectors;
import org.mapstruct.*;

import com.genifast.dms.dto.workflow.WorkflowDTO;
import com.genifast.dms.entity.enums.DocumentType;
import com.genifast.dms.entity.workflow.Workflow;
import com.genifast.dms.entity.workflow.WorkflowDept;
import com.genifast.dms.mapper.bpmn.BpmnUploadMapper;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, uses = { WorkflowEleMapper.class,
    BpmnUploadMapper.class })
public interface WorkflowMapper {
  @Named("mapCodeToDocumentType")
  static DocumentType mapCodeToDocumentType(String code) {
    return DocumentType.fromCode(code);
  }

  @Named("mapDocumentTypeToCode")
  static String mapDocumentTypeToCode(DocumentType type) {
    return type != null ? type.getCode() : null;
  }

  @Named("mapDeptListToIds")
  static List<Long> mapDeptListToIds(List<WorkflowDept> workflowDepts) {
    if (workflowDepts == null)
      return null;
    return workflowDepts.stream().map(wld -> wld.getDepartment().getId()).collect(Collectors.toList());
  }

  @Mapping(source = "bpmnUpload.id", target = "bpmnUploadId")
  @Mapping(source = "bpmnUpload", target = "bpmnUploadDto")
  @Mapping(source = "documentType", target = "documentType", qualifiedByName = "mapDocumentTypeToCode")
  @Mapping(source = "workflowDepts", target = "departmentIds", qualifiedByName = "mapDeptListToIds")
  @Mapping(source = "workflowEle", target = "workflowEleDto")
  WorkflowDTO toDto(Workflow workflow);

  @Mapping(source = "bpmnUploadId", target = "bpmnUpload.id")
  @Mapping(source = "documentType", target = "documentType", qualifiedByName = "mapCodeToDocumentType")
  Workflow toEntity(WorkflowDTO dto);

  List<WorkflowDTO> toDtos(List<Workflow> workflows);
}
