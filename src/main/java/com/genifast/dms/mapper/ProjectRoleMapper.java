package com.genifast.dms.mapper;

import com.genifast.dms.dto.response.ProjectRoleResponse;
import com.genifast.dms.entity.ProjectRole;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, uses = { PermissionMapper.class })
public interface ProjectRoleMapper {
    @Mapping(source = "project.id", target = "projectId")
    ProjectRoleResponse toProjectRoleResponse(ProjectRole projectRole);

    List<ProjectRoleResponse> toProjectRoleResponseList(List<ProjectRole> roles);
}