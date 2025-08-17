package com.genifast.dms.mapper.workflow;

import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;

import com.genifast.dms.common.utils.ConvertUtil;
import com.genifast.dms.dto.workflow.WorkflowEleDTO;
import com.genifast.dms.entity.workflow.WorkflowEle;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface WorkflowEleMapper {
  @Named("mapCategoryIds")
  static List<Long> mapCategoryIds(String categoryIds) {
    return ConvertUtil.stringToLongList(categoryIds);
  }

  @Named("mapEles")
  static List<String> mapEles(String eles) {
    return ConvertUtil.stringToStringList(eles);
  }

  @Named("mapCategoryIdsReverse")
  static String mapCategoryIdsReverse(List<Long> categoryIds) {
    return ConvertUtil.longListToString(categoryIds);
  }

  @Named("mapElesReverse")
  static String mapElesReverse(List<String> eles) {
    return ConvertUtil.stringListToString(eles);
  }

  @Mapping(source = "categoryIds", target = "categoryIds", qualifiedByName = "mapCategoryIds")
  @Mapping(source = "urgency", target = "urgency", qualifiedByName = "mapEles")
  @Mapping(source = "security", target = "security", qualifiedByName = "mapEles")
  WorkflowEleDTO toDto(WorkflowEle workflowEle);

  @Mapping(source = "categoryIds", target = "categoryIds", qualifiedByName = "mapCategoryIdsReverse")
  @Mapping(source = "urgency", target = "urgency", qualifiedByName = "mapElesReverse")
  @Mapping(source = "security", target = "security", qualifiedByName = "mapElesReverse")
  WorkflowEle toEntity(WorkflowEleDTO workflowEleDto);
}
