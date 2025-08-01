package com.genifast.dms.mapper;

import com.genifast.dms.dto.response.PermissionResponseDto;
import com.genifast.dms.entity.Permission;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PermissionMapper {
    PermissionResponseDto toPermissionResponseDto(Permission permission);

    List<PermissionResponseDto> toPermissionResponseDtoList(List<Permission> permissions);
}