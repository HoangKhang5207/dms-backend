package com.genifast.dms.mapper.workflow;

import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import com.genifast.dms.dto.workflow.WorkflowStepsDTO;
import com.genifast.dms.entity.workflow.WorkflowSteps;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface WorkflowStepsMapper {
    WorkflowStepsDTO toDto(WorkflowSteps workflowSteps);

    WorkflowSteps toEntity(WorkflowStepsDTO workflowStepsDto);

    List<WorkflowStepsDTO> toDtos(List<WorkflowSteps> workflowSteps);
}
