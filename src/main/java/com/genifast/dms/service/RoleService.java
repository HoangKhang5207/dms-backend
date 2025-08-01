package com.genifast.dms.service;

import com.genifast.dms.dto.request.RoleRequestDto;
import com.genifast.dms.dto.response.RoleResponseDto;
import java.util.List;

public interface RoleService {
    RoleResponseDto createRole(RoleRequestDto roleDto);

    RoleResponseDto updateRole(Long roleId, RoleRequestDto roleDto);

    void deleteRole(Long roleId);

    List<RoleResponseDto> getAllRolesByOrg(Long orgId);

    RoleResponseDto getRoleById(Long roleId);
}
