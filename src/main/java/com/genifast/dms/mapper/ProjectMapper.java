package com.genifast.dms.mapper;

import com.genifast.dms.dto.response.ProjectResponse;
import com.genifast.dms.entity.Project;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, uses = { ProjectMemberMapper.class,
        ProjectRoleMapper.class })
public interface ProjectMapper {

    @Mapping(source = "organization.id", target = "organizationId")
    @Mapping(source = "project", target = "memberCount", qualifiedByName = "getMemberCount")
    @Mapping(source = "project", target = "documentCount", qualifiedByName = "getDocumentCount")
    ProjectResponse toProjectResponse(Project project);

    @Named("getMemberCount")
    default int getMemberCount(Project project) {
        return project.getMembers() != null ? project.getMembers().size() : 0;
    }

    @Named("getDocumentCount")
    default int getDocumentCount(Project project) {
        return project.getDocuments() != null ? project.getDocuments().size() : 0;
    }
}