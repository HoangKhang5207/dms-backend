package com.genifast.dms.service;

import com.genifast.dms.dto.response.PermissionResponseDto;
import java.util.List;

public interface PermissionService {
    List<PermissionResponseDto> getAllPermissions();
}