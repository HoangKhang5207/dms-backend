package com.genifast.dms.mapper;

import com.genifast.dms.dto.request.RoleRequestDto;
import com.genifast.dms.dto.response.RoleResponseDto;
import com.genifast.dms.entity.Role;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, uses = { PermissionMapper.class })
public interface RoleMapper {

    @Mapping(source = "organization.id", target = "organizationId")
    RoleResponseDto toRoleResponseDto(Role role);

    List<RoleResponseDto> toRoleResponseDtoList(List<Role> roles);

    // Bỏ qua các trường quan hệ sẽ được xử lý thủ công trong service
    @Mapping(target = "organization", ignore = true)
    @Mapping(target = "permissions", ignore = true)
    Role toRole(RoleRequestDto roleDto);
}