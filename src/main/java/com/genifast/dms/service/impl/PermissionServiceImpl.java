package com.genifast.dms.service.impl;

import com.genifast.dms.dto.response.PermissionResponseDto;
import com.genifast.dms.mapper.PermissionMapper;
import com.genifast.dms.repository.PermissionRepository;
import com.genifast.dms.service.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService {

    private final PermissionRepository permissionRepository;
    private final PermissionMapper permissionMapper;

    @Override
    public List<PermissionResponseDto> getAllPermissions() {
        return permissionMapper.toPermissionResponseDtoList(permissionRepository.findAll());
    }
}