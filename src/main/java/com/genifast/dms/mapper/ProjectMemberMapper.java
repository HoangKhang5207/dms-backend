package com.genifast.dms.mapper;

import com.genifast.dms.dto.response.ProjectMemberResponse;
import com.genifast.dms.entity.ProjectMember;
import com.genifast.dms.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ProjectMemberMapper {
    @Mapping(source = "id", target = "id") // Map ID của ProjectMember
    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "user.email", target = "userEmail")
    @Mapping(source = "user", target = "userFullName", qualifiedByName = "getFullName")
    @Mapping(source = "projectRole.id", target = "projectRoleId")
    @Mapping(source = "projectRole.name", target = "projectRoleName")
    ProjectMemberResponse toProjectMemberResponse(ProjectMember projectMember);

    @Named("getFullName")
    default String getFullName(User user) {
        if (user == null) {
            return null;
        }
        return (user.getLastName() != null ? user.getLastName() : "") + " " +
                (user.getFirstName() != null ? user.getFirstName() : "");
    }
}